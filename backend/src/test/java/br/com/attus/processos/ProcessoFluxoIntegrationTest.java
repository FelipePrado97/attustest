package br.com.attus.processos;

import br.com.attus.processos.application.port.in.AtualizarProcessoUseCase;
import br.com.attus.processos.config.KafkaConfig;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.model.DadosProcesso;
import br.com.attus.processos.shared.CorrelationId;
import br.com.attus.processos.support.CnjFixture;
import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class ProcessoFluxoIntegrationTest {

    private static final String URL = "/api/v1/processos";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private AtualizarProcessoUseCase atualizarProcesso;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${app.kafka.topico-eventos}")
    private String topico;

    @Test
    void cadastroGeraHistoricoAssincronoViaKafka() throws Exception {
        var id = cadastrar(CnjFixture.proximo(), "Execução fiscal - IPTU");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get(URL + "/" + id + "/historico"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(1)))
                        .andExpect(jsonPath("$[0].tipo").value("CADASTRADO")));

        var pendentes = jdbc.queryForObject(
                "select count(*) from outbox_evento where agregado_id = ? and publicado_em is null", Long.class, id);
        assertThat(pendentes).isZero();
    }

    @Test
    void cicloCompletoDeAlteracoesFicaRegistradoNaLinhaDoTempo() throws Exception {
        var id = cadastrar(CnjFixture.proximo(), "Cobrança de dívida ativa");

        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assunto": "Cobrança de dívida ativa - ISS", "parteContraria": "Empresa X",
                         "valorCausa": 2500.10, "dataDistribuicao": "2024-01-10", "versao": 0}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.valorCausa").value(2500.10));

        mvc.perform(patch(URL + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ARQUIVADO\", \"versao\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARQUIVADO"))
                .andExpect(jsonPath("$.transicoesPermitidas[0]").value("ATIVO"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get(URL + "/" + id + "/historico"))
                        .andExpect(jsonPath("$", hasSize(3)))
                        .andExpect(jsonPath("$[0].tipo").value("STATUS_ALTERADO")));
    }

    @Test
    void edicaoComVersaoAntigaRetornaConflito() throws Exception {
        var id = cadastrar(CnjFixture.proximo(), "Assunto original");
        var corpo = """
                {"assunto": "Edição %s", "parteContraria": "Parte", "valorCausa": 10, "dataDistribuicao": "2024-01-10", "versao": 0}
                """;

        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON).content(corpo.formatted("A")))
                .andExpect(status().isOk());

        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON).content(corpo.formatted("B")))
                .andExpect(status().isConflict());

        mvc.perform(get(URL + "/" + id)).andExpect(jsonPath("$.assunto").value("Edição A"));
    }

    @Test
    void numeroCnjDuplicadoMesmoComOutraMascaraRetornaConflito() throws Exception {
        var numero = CnjFixture.proximo();
        cadastrar(numero, "Primeiro");

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo(numero.replaceAll("\\D", ""), "Segundo")))
                .andExpect(status().isConflict());
    }

    @Test
    void cnjComDigitoVerificadorInvalidoRetorna400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo("0001234-72.2024.8.26.0100", "X")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("numeroCnj"));
    }

    @Test
    void dataDeDistribuicaoComAnoAbsurdoRetorna400NoCampo() throws Exception {
        var corpo = corpo(CnjFixture.proximo(), "Ano digitado errado").replace("2024-03-15", "0004-04-11");

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("dataDistribuicao"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Data de distribuição não pode ser anterior a 01/01/1900"));
    }

    @Test
    void listagemFiltraPorTermoEStatus() throws Exception {
        var marcador = "Filtro-" + UUID.randomUUID().toString().substring(0, 8);
        var idAtivo = cadastrar(CnjFixture.proximo(), marcador + " ativo");
        var idSuspenso = cadastrar(CnjFixture.proximo(), marcador + " suspenso");
        mvc.perform(patch(URL + "/" + idSuspenso + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"SUSPENSO\", \"versao\": 0}")).andExpect(status().isOk());

        mvc.perform(get(URL).param("termo", marcador.toLowerCase()))
                .andExpect(jsonPath("$.totalItens").value(2));
        mvc.perform(get(URL).param("termo", marcador).param("status", "ATIVO"))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(idAtivo.toString()));
    }

    @Test
    void exclusaoRemoveProcessoMasMantemHistorico() throws Exception {
        var id = cadastrar(CnjFixture.proximo(), "Para excluir");

        mvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(URL + "/" + id)).andExpect(status().isNotFound());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get(URL + "/" + id + "/historico"))
                        .andExpect(jsonPath("$", hasSize(2)))
                        .andExpect(jsonPath("$[0].tipo").value("EXCLUIDO")));
    }

    @Test
    void correlationIdDaRequisicaoViajaAteOOutbox() throws Exception {
        var resposta = mvc.perform(post(URL).header(CorrelationId.HEADER, "cid-integracao-0001")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(CnjFixture.proximo(), "Rastreável")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var id = UUID.fromString(JsonPath.read(resposta, "$.id"));

        var correlationId = jdbc.queryForObject(
                "select correlation_id from outbox_evento where agregado_id = ?", String.class, id);
        assertThat(correlationId).isEqualTo("cid-integracao-0001");
    }

    @Test
    void mensagemInvalidaVaiParaDeadLetterTopicSemTravarOConsumidor() throws Exception {
        kafkaTemplate.send(topico, "chave-invalida", "{ isto não é json").get();

        try (var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "teste-dlt",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(topico + KafkaConfig.SUFIXO_DLT));
            var record = KafkaTestUtils.getSingleRecord(consumer, topico + KafkaConfig.SUFIXO_DLT, Duration.ofSeconds(15));

            assertThat(record.key()).isEqualTo("chave-invalida");
            assertThat(record.headers().lastHeader("kafka_dlt-exception-fqcn")).isNotNull();
        }

        var id = cadastrar(CnjFixture.proximo(), "Depois do veneno");
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get(URL + "/" + id + "/historico")).andExpect(jsonPath("$", hasSize(1))));
    }

    @Test
    void buscaPorNumeroSemMascaraEncontraOProcesso() throws Exception {
        var numero = CnjFixture.proximo();
        var id = cadastrar(numero, "Busca sem máscara");

        mvc.perform(get(URL).param("termo", numero.replaceAll("\\D", "")))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(id.toString()));
    }

    @Test
    void edicoesSimultaneasNaMesmaVersao_exatamenteUmaVence() throws Exception {
        var id = cadastrar(CnjFixture.proximo(), "Concorrência");
        var largada = new CountDownLatch(1);
        Callable<String> editar = () -> {
            largada.await();
            try {
                atualizarProcesso.atualizar(new AtualizarProcessoUseCase.Comando(id, 0L,
                        new DadosProcesso("Editado em paralelo", "Parte", BigDecimal.TEN, LocalDate.of(2024, 1, 1))));
                return "ok";
            } catch (ConflitoException e) {
                return "conflito";
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeiro = executor.submit(editar);
            var segundo = executor.submit(editar);
            largada.countDown();

            assertThat(List.of(primeiro.get(15, TimeUnit.SECONDS), segundo.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("ok", "conflito");
        }
        mvc.perform(get(URL + "/" + id)).andExpect(jsonPath("$.versao").value(1));
    }

    private UUID cadastrar(String numero, String assunto) throws Exception {
        var resposta = mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo(numero, assunto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    private static String corpo(String numero, String assunto) {
        return """
                {"numeroCnj": "%s", "assunto": "%s", "parteContraria": "Contribuinte Exemplo",
                 "valorCausa": 1500.00, "dataDistribuicao": "2024-03-15"}
                """.formatted(numero, assunto);
    }
}
