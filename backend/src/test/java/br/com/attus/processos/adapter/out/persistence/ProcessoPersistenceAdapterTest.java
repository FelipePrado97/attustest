package br.com.attus.processos.adapter.out.persistence;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.model.DadosProcesso;
import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.Processo;
import br.com.attus.processos.domain.model.StatusProcesso;
import br.com.attus.processos.support.CnjFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ProcessoPersistenceAdapter.class)
class ProcessoPersistenceAdapterTest {

    @Autowired
    private ProcessoPersistenceAdapter adapter;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void salvaERecuperaTodosOsCampos() {
        var novo = Processo.novo(NumeroCnj.of(CnjFixture.proximo()),
                new DadosProcesso("Execução fiscal", "Contribuinte", new BigDecimal("1234.56"), LocalDate.of(2024, 1, 31)), CLOCK);

        var salvo = adapter.salvar(novo);
        var lido = adapter.buscarPorId(novo.getId()).orElseThrow();

        assertThat(salvo.getVersao()).isZero();
        assertThat(lido.getNumero()).isEqualTo(novo.getNumero());
        assertThat(lido.getAssunto()).isEqualTo("Execução fiscal");
        assertThat(lido.getParteContraria()).isEqualTo("Contribuinte");
        assertThat(lido.getValorCausa()).isEqualByComparingTo("1234.56");
        assertThat(lido.getDataDistribuicao()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(lido.getStatus()).isEqualTo(StatusProcesso.ATIVO);
        assertThat(lido.getCriadoEm()).isEqualTo(novo.getCriadoEm());
    }

    @Test
    void gravaONumeroSemMascaraParaBusca() {
        var numero = CnjFixture.proximo();
        adapter.salvar(novo(numero, "Qualquer"));

        var digitos = jdbc.queryForObject("select numero_cnj_digitos from processo where numero_cnj = ?", String.class, numero);

        assertThat(digitos).isEqualTo(NumeroCnj.somenteDigitos(numero));
    }

    @Test
    void atualizacaoIncrementaAVersao() {
        var salvo = adapter.salvar(novo(CnjFixture.proximo(), "Original"));
        salvo.alterarStatus(StatusProcesso.SUSPENSO, CLOCK);

        var atualizado = adapter.salvar(salvo);

        assertThat(atualizado.getVersao()).isEqualTo(1L);
        assertThat(atualizado.getStatus()).isEqualTo(StatusProcesso.SUSPENSO);
    }

    @Test
    void existePorNumero() {
        var numero = CnjFixture.proximo();
        adapter.salvar(novo(numero, "Qualquer"));

        assertThat(adapter.existePorNumero(NumeroCnj.of(numero))).isTrue();
        assertThat(adapter.existePorNumero(NumeroCnj.of(CnjFixture.proximo()))).isFalse();
    }

    @Test
    void numeroDuplicadoNoBancoViraConflitoDeDominio() {
        var numero = CnjFixture.proximo();
        adapter.salvar(novo(numero, "Primeiro"));

        assertThatThrownBy(() -> adapter.salvar(novo(numero, "Segundo")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining(numero);
    }

    @Test
    void excluirRemoveEIgnoraIdInexistente() {
        var salvo = adapter.salvar(novo(CnjFixture.proximo(), "Para excluir"));

        adapter.excluir(salvo.getId());
        adapter.excluir(UUID.randomUUID());

        assertThat(adapter.buscarPorId(salvo.getId())).isEmpty();
    }

    @Test
    void filtraPorStatus() {
        var marcador = marcador();
        adapter.salvar(novo(CnjFixture.proximo(), marcador + " ativo"));
        var suspenso = adapter.salvar(novo(CnjFixture.proximo(), marcador + " suspenso"));
        suspenso.alterarStatus(StatusProcesso.SUSPENSO, CLOCK);
        adapter.salvar(suspenso);

        var pagina = adapter.buscar(new FiltroProcessos(marcador, StatusProcesso.SUSPENSO), 0, 10);

        assertThat(pagina.itens()).extracting(Processo::getId).containsExactly(suspenso.getId());
    }

    @Test
    void termoBuscaAssuntoEParteSemDiferenciarMaiusculas() {
        var marcador = marcador();
        var porAssunto = adapter.salvar(novo(CnjFixture.proximo(), "Execução " + marcador));
        var porParte = adapter.salvar(Processo.novo(NumeroCnj.of(CnjFixture.proximo()),
                new DadosProcesso("Outro assunto", "Empresa " + marcador, BigDecimal.ONE, LocalDate.of(2024, 1, 1)), CLOCK));

        var pagina = adapter.buscar(new FiltroProcessos(marcador.toUpperCase(), null), 0, 10);

        assertThat(pagina.itens()).extracting(Processo::getId).containsExactlyInAnyOrder(porAssunto.getId(), porParte.getId());
    }

    @Test
    void termoNumericoEncontraOCnjComOuSemMascaraEParcial() {
        var numero = CnjFixture.proximo();
        var salvo = adapter.salvar(novo(numero, "Busca por número"));

        for (var termo : new String[]{numero, NumeroCnj.somenteDigitos(numero), numero.substring(0, 10)}) {
            assertThat(adapter.buscar(new FiltroProcessos(termo, null), 0, 50).itens())
                    .as("termo %s", termo)
                    .extracting(Processo::getId)
                    .contains(salvo.getId());
        }
    }

    @Test
    void termoTextualComNumeroNaoCasaComDigitosDoCnj() {
        var numero = CnjFixture.proximo();
        adapter.salvar(novo(numero, "Sem relação"));

        assertThat(adapter.buscar(new FiltroProcessos("IPTU 2024", null), 0, 50).itens())
                .extracting(p -> p.getNumero().valor())
                .doesNotContain(numero);
    }

    @Test
    void curingasDoLikeSaoTratadosComoTextoLiteral() {
        var marcador = marcador();
        var comPercentual = adapter.salvar(novo(CnjFixture.proximo(), marcador + " juros 100% correção"));
        adapter.salvar(novo(CnjFixture.proximo(), marcador + " juros 1000 correção"));

        var pagina = adapter.buscar(new FiltroProcessos("100%", null), 0, 50);

        assertThat(pagina.itens()).extracting(Processo::getId).containsExactly(comPercentual.getId());
    }

    @Test
    void maisRecenteAtualizadoVemPrimeiro() {
        var marcador = marcador();
        var antigo = adapter.salvar(novo(CnjFixture.proximo(), marcador + " antigo"));
        var recente = adapter.salvar(Processo.novo(NumeroCnj.of(CnjFixture.proximo()),
                new DadosProcesso(marcador + " recente", "Parte", BigDecimal.ONE, LocalDate.of(2024, 1, 1)),
                Clock.offset(CLOCK, Duration.ofMinutes(5))));

        assertThat(adapter.buscar(new FiltroProcessos(marcador, null), 0, 10).itens())
                .extracting(Processo::getId)
                .containsExactly(recente.getId(), antigo.getId());
    }

    @Test
    void paginacaoEstavelMesmoComAtualizadoEmEmpatado() {
        var marcador = marcador();
        for (int i = 0; i < 5; i++) {
            adapter.salvar(novo(CnjFixture.proximo(), marcador + " " + i));
        }

        var ids = new ArrayList<UUID>();
        for (int pagina = 0; pagina < 3; pagina++) {
            var resultado = adapter.buscar(new FiltroProcessos(marcador, null), pagina, 2);
            assertThat(resultado.totalItens()).isEqualTo(5);
            assertThat(resultado.totalPaginas()).isEqualTo(3);
            resultado.itens().forEach(p -> ids.add(p.getId()));
        }

        assertThat(ids).hasSize(5).doesNotHaveDuplicates();
    }

    private static Processo novo(String numero, String assunto) {
        return Processo.novo(NumeroCnj.of(numero),
                new DadosProcesso(assunto, "Contribuinte", BigDecimal.TEN, LocalDate.of(2024, 1, 1)), CLOCK);
    }

    private static String marcador() {
        return "m" + UUID.randomUUID().toString().substring(0, 8);
    }
}
