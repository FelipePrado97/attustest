package br.com.attus.processos.adapter.out.messaging;

import br.com.attus.processos.shared.CorrelationId;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final String TOPICO = "processos.eventos";
    private static final Instant AGORA = Instant.parse("2025-06-10T15:00:00Z");

    @Mock
    private OutboxEventoRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final Map<String, CompletableFuture<SendResult<String, String>>> respostas = new HashMap<>();
    private final List<ProducerRecord<String, String>> enviados = new ArrayList<>();
    private final List<String> correlationIdsDuranteEnvio = new ArrayList<>();

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        var properties = new OutboxProperties(Duration.ofSeconds(1), Duration.ofMinutes(5), Duration.ofDays(7));
        relay = new OutboxRelay(repository, kafkaTemplate, properties, Clock.fixed(AGORA, ZoneOffset.UTC), TOPICO);
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(kafkaTemplate.send(any(ProducerRecord.class))).thenAnswer(inv -> {
            ProducerRecord<String, String> record = inv.getArgument(0);
            enviados.add(record);
            correlationIdsDuranteEnvio.add(MDC.get(CorrelationId.MDC_KEY));
            return respostas.getOrDefault(record.key(), sucesso(record));
        });
    }

    @AfterEach
    void limparInterrupcao() {
        Thread.interrupted();
    }

    @Test
    void publicaNaOrdemComChaveDoProcessoEHeaders() {
        var processoA = UUID.randomUUID();
        var processoB = UUID.randomUUID();
        var a1 = evento(processoA, "cid-aaaa-0001");
        var b1 = evento(processoB, null);
        pendentes(a1, b1);

        relay.publicarPendentes();

        assertThat(enviados).extracting(ProducerRecord::key).containsExactly(processoA.toString(), processoB.toString());
        var primeiro = enviados.getFirst();
        assertThat(primeiro.topic()).isEqualTo(TOPICO);
        assertThat(primeiro.value()).isEqualTo(a1.getPayload());
        assertThat(header(primeiro, CorrelationId.HEADER)).isEqualTo("cid-aaaa-0001");
        assertThat(header(primeiro, OutboxRelay.HEADER_EVENTO_ID)).isEqualTo(a1.getId().toString());
        assertThat(header(primeiro, OutboxRelay.HEADER_TIPO)).isEqualTo("CADASTRADO");
        assertThat(enviados.get(1).headers().lastHeader(CorrelationId.HEADER)).isNull();
        assertThat(a1.getPublicadoEm()).isEqualTo(AGORA);
        assertThat(b1.getPublicadoEm()).isEqualTo(AGORA);
    }

    @Test
    void falhaDoEventoBloqueiaSoOProcessoAfetadoEAgendaBackoff() {
        var processoA = UUID.randomUUID();
        var processoB = UUID.randomUUID();
        var a1 = evento(processoA, null);
        var a2 = evento(processoA, null);
        var b1 = evento(processoB, null);
        pendentes(a1, a2, b1);
        respostas.put(processoA.toString(), CompletableFuture.failedFuture(new RecordTooLargeException("mensagem grande demais")));

        relay.publicarPendentes();

        assertThat(enviados).extracting(ProducerRecord::key).containsExactly(processoA.toString(), processoB.toString());
        assertThat(a1.getPublicadoEm()).isNull();
        assertThat(a1.getTentativas()).isEqualTo(1);
        assertThat(a1.getProximaTentativaEm()).isEqualTo(AGORA.plusSeconds(1));
        assertThat(a1.getUltimoErro()).startsWith("RecordTooLargeException: mensagem grande demais");
        assertThat(a2.getPublicadoEm()).as("preserva a ordem do processo A").isNull();
        assertThat(a2.getTentativas()).isZero();
        assertThat(b1.getPublicadoEm()).as("processo B não é afetado").isEqualTo(AGORA);
    }

    @Test
    void backoffCresceAcadaFalhaDoMesmoEvento() {
        var processoA = UUID.randomUUID();
        var a1 = evento(processoA, null);
        a1.registrarFalha("falha anterior", AGORA.minusSeconds(1));
        pendentes(a1);
        respostas.put(processoA.toString(), CompletableFuture.failedFuture(new RecordTooLargeException("de novo")));

        relay.publicarPendentes();

        assertThat(a1.getTentativas()).isEqualTo(2);
        assertThat(a1.getProximaTentativaEm()).isEqualTo(AGORA.plusSeconds(2));
    }

    @Test
    void brokerIndisponivelInterrompeOCicloSemAgendarBackoff() {
        var a1 = evento(UUID.randomUUID(), null);
        var b1 = evento(UUID.randomUUID(), null);
        pendentes(a1, b1);
        respostas.put(a1.getAgregadoId().toString(), CompletableFuture.failedFuture(new TimeoutException("Expiring 1 record")));

        relay.publicarPendentes();

        assertThat(enviados).hasSize(1);
        assertThat(a1.getTentativas()).isEqualTo(1);
        assertThat(a1.getProximaTentativaEm()).as("não é culpa do evento").isNull();
        assertThat(b1.getPublicadoEm()).isNull();
    }

    @Test
    void incidenteEventoGrandeDemais_registraACausaRaizEBloqueiaSoOProcessoAfetado() {
        var processoComEventoGrande = UUID.randomUUID();
        var eventoGrande = evento(processoComEventoGrande, "carga-legado-lote-0917");
        var edicaoPosterior = evento(processoComEventoGrande, "inc-edicao-processo-a");
        var cadastroDeOutroProcesso = evento(UUID.randomUUID(), "inc-cadastro-processo-c");
        pendentes(eventoGrande, edicaoPosterior, cadastroDeOutroProcesso);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenAnswer(inv -> {
            ProducerRecord<String, String> record = inv.getArgument(0);
            if (record.key().equals(processoComEventoGrande.toString())) {
                throw new KafkaException("Send failed", new RecordTooLargeException(
                        "The message is 1600268 bytes when serialized which is larger than 1048576"));
            }
            return sucesso(record);
        });

        relay.publicarPendentes();

        assertThat(eventoGrande.getUltimoErro()).startsWith("RecordTooLargeException: The message is 1600268 bytes");
        assertThat(eventoGrande.getProximaTentativaEm()).isEqualTo(AGORA.plusSeconds(1));
        assertThat(edicaoPosterior.getPublicadoEm()).as("mantém a ordem do processo afetado").isNull();
        assertThat(cadastroDeOutroProcesso.getPublicadoEm()).as("outros processos não são afetados").isEqualTo(AGORA);
    }

    @Test
    void falhaSincronaAoEnviarTambemEhTratada() {
        var a1 = evento(UUID.randomUUID(), null);
        pendentes(a1);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenThrow(new KafkaException("falha ao obter metadados", new TimeoutException("metadata")));

        relay.publicarPendentes();

        assertThat(a1.getTentativas()).isEqualTo(1);
        assertThat(a1.getPublicadoEm()).isNull();
    }

    @Test
    void eventoEmBackoffEhPuladoJuntoComOsSeguintesDoMesmoProcesso() {
        var processoA = UUID.randomUUID();
        var a1 = evento(processoA, null);
        a1.registrarFalha("falha anterior", AGORA.plusSeconds(30));
        var a2 = evento(processoA, null);
        var b1 = evento(UUID.randomUUID(), null);
        pendentes(a1, a2, b1);

        relay.publicarPendentes();

        assertThat(enviados).extracting(ProducerRecord::key).containsExactly(b1.getAgregadoId().toString());
        assertThat(a1.getTentativas()).as("não conta tentativa enquanto espera").isEqualTo(1);
    }

    @Test
    void interrupcaoDaThreadEncerraOCicloEPreservaOFlag() {
        var a1 = evento(UUID.randomUUID(), null);
        var b1 = evento(UUID.randomUUID(), null);
        pendentes(a1, b1);
        respostas.put(a1.getAgregadoId().toString(), new CompletableFuture<>());
        Thread.currentThread().interrupt();

        relay.publicarPendentes();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        assertThat(enviados).hasSize(1);
        verify(repository, never()).save(any());
    }

    @Test
    void correlationIdDoEventoFicaNoMdcSoDuranteOEnvio() {
        pendentes(evento(UUID.randomUUID(), "cid-bbbb-0002"));

        relay.publicarPendentes();

        assertThat(correlationIdsDuranteEnvio).containsExactly("cid-bbbb-0002");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void erroMuitoLongoEhTruncado() {
        var a1 = evento(UUID.randomUUID(), null);
        pendentes(a1);
        respostas.put(a1.getAgregadoId().toString(),
                CompletableFuture.failedFuture(new RecordTooLargeException("x".repeat(5_000))));

        relay.publicarPendentes();

        assertThat(a1.getUltimoErro()).hasSize(OutboxEventoEntity.TAMANHO_MAXIMO_ERRO);
    }

    @Test
    void semPendentesNaoEnviaNada() {
        pendentes();

        relay.publicarPendentes();

        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
    }

    private void pendentes(OutboxEventoEntity... eventos) {
        when(repository.findTop500ByPublicadoEmIsNullOrderBySequenciaAsc()).thenReturn(List.of(eventos));
    }

    private static OutboxEventoEntity evento(UUID processoId, String correlationId) {
        return new OutboxEventoEntity(UUID.randomUUID(), processoId, "CADASTRADO", "{\"processoId\":\"" + processoId + "\"}",
                correlationId, AGORA);
    }

    private static CompletableFuture<SendResult<String, String>> sucesso(ProducerRecord<String, String> record) {
        var metadata = new RecordMetadata(new TopicPartition(record.topic(), 0), 0, 0, 0, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
    }

    private static String header(ProducerRecord<String, String> record, String nome) {
        return new String(record.headers().lastHeader(nome).value(), StandardCharsets.UTF_8);
    }
}
