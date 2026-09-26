package br.com.attus.processos.domain.model;

import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.exception.DadoInvalidoException;
import br.com.attus.processos.domain.exception.ProcessoArquivadoException;
import br.com.attus.processos.domain.exception.TransicaoStatusInvalidaException;
import br.com.attus.processos.support.CnjFixture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

class ProcessoTest {

    private static final NumeroCnj NUMERO = NumeroCnj.of("0001234-71.2024.8.26.0100");
    private static final LocalDate DATA_VALIDA = LocalDate.of(2024, 3, 15);

    @Nested
    class Cadastro {

        @Test
        void nasceAtivoERegistraEventoDeCadastro() {
            var processo = Processo.novo(NUMERO, dados("1000.5"), CLOCK);

            assertThat(processo.getStatus()).isEqualTo(StatusProcesso.ATIVO);
            assertThat(processo.getVersao()).isNull();
            assertThat(processo.getCriadoEm()).isEqualTo(processo.getAtualizadoEm()).isEqualTo(Instant.now(CLOCK));
            assertThat(processo.extrairEventos())
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.tipo()).isEqualTo(TipoEvento.CADASTRADO);
                        assertThat(e.processoId()).isEqualTo(processo.getId());
                        assertThat(e.numeroCnj()).isEqualTo(NUMERO.valor());
                    });
        }

        @Test
        void normalizaEspacosDosTextos() {
            var processo = Processo.novo(NUMERO, new DadosProcesso("  Execução fiscal  ", " Fulano ", BigDecimal.ONE, DATA_VALIDA), CLOCK);

            assertThat(processo.getAssunto()).isEqualTo("Execução fiscal");
            assertThat(processo.getParteContraria()).isEqualTo("Fulano");
        }

        @Test
        void dadosNulosSaoRejeitadosComoDadoInvalido() {
            assertThatThrownBy(() -> Processo.novo(NUMERO, null, CLOCK))
                    .isInstanceOf(DadoInvalidoException.class)
                    .extracting(e -> ((DadoInvalidoException) e).getCampo())
                    .isEqualTo("dados");
        }
    }

    @Nested
    class ValidacaoDeTextos {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void assuntoEmBrancoEhRejeitado(String assunto) {
            assertCampoInvalido(new DadosProcesso(assunto, "Parte", BigDecimal.TEN, DATA_VALIDA), "assunto");
        }

        @Test
        void assuntoNuloEhRejeitado() {
            assertCampoInvalido(new DadosProcesso(null, "Parte", BigDecimal.TEN, DATA_VALIDA), "assunto");
        }

        @Test
        void assuntoAcimaDoLimiteEhRejeitado() {
            var longo = "a".repeat(Processo.TAMANHO_MAXIMO_ASSUNTO + 1);
            assertCampoInvalido(new DadosProcesso(longo, "Parte", BigDecimal.TEN, DATA_VALIDA), "assunto");
        }

        @Test
        void assuntoNoLimiteEhAceito() {
            var noLimite = "a".repeat(Processo.TAMANHO_MAXIMO_ASSUNTO);
            assertThat(Processo.novo(NUMERO, new DadosProcesso(noLimite, "Parte", BigDecimal.TEN, DATA_VALIDA), CLOCK).getAssunto())
                    .hasSize(Processo.TAMANHO_MAXIMO_ASSUNTO);
        }

        @Test
        void parteContrariaAcimaDoLimiteEhRejeitada() {
            var longa = "p".repeat(Processo.TAMANHO_MAXIMO_PARTE + 1);
            assertCampoInvalido(new DadosProcesso("Assunto", longa, BigDecimal.TEN, DATA_VALIDA), "parteContraria");
        }
    }

    @Nested
    class ValidacaoDoValorDaCausa {

        @Test
        void normalizaParaDuasCasasDecimais() {
            assertThat(Processo.novo(NUMERO, dados("1000.5"), CLOCK).getValorCausa()).isEqualTo(new BigDecimal("1000.50"));
        }

        @Test
        void aceitaZerosFinaisAlemDaSegundaCasa() {
            assertThat(Processo.novo(NUMERO, dados("10.500"), CLOCK).getValorCausa()).isEqualTo(new BigDecimal("10.50"));
        }

        @Test
        void aceitaZero() {
            assertThat(Processo.novo(NUMERO, dados("0"), CLOCK).getValorCausa()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void aceitaOMaiorValorQueCabeNaColuna() {
            assertThat(Processo.novo(NUMERO, dados("9999999999999.99"), CLOCK).getValorCausa())
                    .isEqualTo(new BigDecimal("9999999999999.99"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-0.01", "10.555", "10000000000000.00"})
        void rejeitaNegativoMaisDeDuasCasasOuAcimaDoLimite(String valor) {
            assertCampoInvalido(dados(valor), "valorCausa");
        }

        @Test
        void rejeitaNulo() {
            assertCampoInvalido(new DadosProcesso("Assunto", "Parte", null, DATA_VALIDA), "valorCausa");
        }
    }

    @Nested
    class ValidacaoDaDataDeDistribuicao {

        @Test
        void aceitaHojeNoFusoDeBrasilia() {
            var hoje = LocalDate.of(2025, 6, 10);
            assertThat(Processo.novo(NUMERO, new DadosProcesso("A", "P", BigDecimal.TEN, hoje), CLOCK).getDataDistribuicao())
                    .isEqualTo(hoje);
        }

        @Test
        void rejeitaDataFutura() {
            assertCampoInvalido(new DadosProcesso("A", "P", BigDecimal.TEN, LocalDate.of(2025, 6, 11)), "dataDistribuicao");
        }

        @Test
        void rejeitaNula() {
            assertCampoInvalido(new DadosProcesso("A", "P", BigDecimal.TEN, null), "dataDistribuicao");
        }

        @Test
        void aceitaOPrimeiroDiaDoAnoDeAjuizamento() {
            var inicioDoAno = LocalDate.of(2024, 1, 1);
            assertThat(Processo.novo(NUMERO, new DadosProcesso("A", "P", BigDecimal.TEN, inicioDoAno), CLOCK).getDataDistribuicao())
                    .isEqualTo(inicioDoAno);
        }

        @Test
        void rejeitaDataAnteriorAoAnoDeAjuizamentoDoCnj() {
            assertThatThrownBy(() -> Processo.novo(NUMERO, new DadosProcesso("A", "P", BigDecimal.TEN, LocalDate.of(2023, 12, 31)), CLOCK))
                    .isInstanceOf(DadoInvalidoException.class)
                    .hasMessage("Data de distribuição não pode ser anterior ao ano de ajuizamento do processo (2024)");
        }

        @Test
        void rejeitaAnoAbsurdoDigitadoPorEngano() {
            assertThatThrownBy(() -> Processo.novo(NUMERO, new DadosProcesso("A", "P", BigDecimal.TEN, LocalDate.of(4, 4, 11)), CLOCK))
                    .isInstanceOf(DadoInvalidoException.class)
                    .hasMessage("Data de distribuição não pode ser anterior a 01/01/1900");
        }

        @Test
        void pisoDe1900ValeMesmoParaCnjComAnoMaisAntigo() {
            var cnjDe1850 = NumeroCnj.of(CnjFixture.comAno(1850));

            assertThatThrownBy(() -> Processo.novo(cnjDe1850, new DadosProcesso("A", "P", BigDecimal.TEN, LocalDate.of(1899, 12, 31)), CLOCK))
                    .isInstanceOf(DadoInvalidoException.class)
                    .hasMessageContaining("01/01/1900");
        }

        @Test
        void edicaoTambemRespeitaOAnoDeAjuizamento() {
            var processo = novoSemEventos();

            assertThatThrownBy(() -> processo.atualizar(new DadosProcesso("A", "P", BigDecimal.TEN, LocalDate.of(2020, 5, 1)), CLOCK))
                    .isInstanceOf(DadoInvalidoException.class)
                    .hasMessageContaining("ano de ajuizamento");
            assertThat(processo.getDataDistribuicao()).isEqualTo(DATA_VALIDA);
        }

        @Test
        void reconstituicaoNaoReaplicaAsRegrasDeIntervalo() {
            var futura = LocalDate.now().plusYears(1);
            var processo = Processo.reconstituir(UUID.randomUUID(), NUMERO, new DadosProcesso("A", "P", BigDecimal.TEN, futura),
                    StatusProcesso.ATIVO, 0L, Instant.now(CLOCK), Instant.now(CLOCK));

            assertThat(processo.getDataDistribuicao()).isEqualTo(futura);
            assertThat(processo.extrairEventos()).isEmpty();
        }
    }

    @Nested
    class Atualizacao {

        @Test
        void aplicaNovosDadosERegistraEvento() {
            var processo = novoSemEventos();

            processo.atualizar(new DadosProcesso("Novo assunto", "Nova parte", new BigDecimal("99.90"), LocalDate.of(2024, 2, 2)), CLOCK);

            assertThat(processo.getAssunto()).isEqualTo("Novo assunto");
            assertThat(processo.getParteContraria()).isEqualTo("Nova parte");
            assertThat(processo.getValorCausa()).isEqualTo(new BigDecimal("99.90"));
            assertThat(processo.extrairEventos()).extracting(ProcessoEvento::tipo).containsExactly(TipoEvento.ATUALIZADO);
        }

        @Test
        void dadoInvalidoNaoAlteraNadaNoAgregado() {
            var processo = novoSemEventos();

            assertThatThrownBy(() -> processo.atualizar(new DadosProcesso("Novo assunto", "Parte", new BigDecimal("-1"), DATA_VALIDA), CLOCK))
                    .isInstanceOf(DadoInvalidoException.class);

            assertThat(processo.getAssunto()).isEqualTo("Execução fiscal - IPTU");
            assertThat(processo.extrairEventos()).isEmpty();
        }

        @Test
        void processoArquivadoNaoPodeSerEditado() {
            var processo = novoSemEventos();
            processo.alterarStatus(StatusProcesso.ARQUIVADO, CLOCK);

            assertThatThrownBy(() -> processo.atualizar(dados("20"), CLOCK)).isInstanceOf(ProcessoArquivadoException.class);
        }

        @Test
        void processoSuspensoPodeSerEditado() {
            var processo = novoSemEventos();
            processo.alterarStatus(StatusProcesso.SUSPENSO, CLOCK);

            processo.atualizar(dados("20"), CLOCK);

            assertThat(processo.getValorCausa()).isEqualTo(new BigDecimal("20.00"));
        }
    }

    @Nested
    class AlteracaoDeStatus {

        @Test
        void registraEventoComOrigemEDestino() {
            var processo = novoSemEventos();

            processo.alterarStatus(StatusProcesso.SUSPENSO, CLOCK);

            assertThat(processo.getStatus()).isEqualTo(StatusProcesso.SUSPENSO);
            assertThat(processo.extrairEventos())
                    .extracting(ProcessoEvento::tipo, ProcessoEvento::descricao)
                    .containsExactly(tuple(TipoEvento.STATUS_ALTERADO, "Status alterado de ATIVO para SUSPENSO"));
        }

        @Test
        void rejeitaTransicaoInvalida() {
            var processo = novoSemEventos();
            processo.alterarStatus(StatusProcesso.ARQUIVADO, CLOCK);

            assertThatThrownBy(() -> processo.alterarStatus(StatusProcesso.SUSPENSO, CLOCK))
                    .isInstanceOf(TransicaoStatusInvalidaException.class)
                    .hasMessageContaining("ARQUIVADO -> SUSPENSO");
        }

        @Test
        void rejeitaStatusNulo() {
            assertThatThrownBy(() -> novoSemEventos().alterarStatus(null, CLOCK))
                    .isInstanceOf(DadoInvalidoException.class);
        }
    }

    @Nested
    class Eventos {

        @Test
        void extrairEsvaziaALista() {
            var processo = Processo.novo(NUMERO, dados("10"), CLOCK);
            processo.extrairEventos();

            assertThat(processo.extrairEventos()).isEmpty();
        }

        @Test
        void exclusaoRegistraEvento() {
            var processo = novoSemEventos();

            processo.marcarExclusao(CLOCK);

            assertThat(processo.extrairEventos()).extracting(ProcessoEvento::tipo).containsExactly(TipoEvento.EXCLUIDO);
        }
    }

    private static Processo novoSemEventos() {
        var processo = Processo.novo(NUMERO, dados("10"), CLOCK);
        processo.extrairEventos();
        return processo;
    }

    private static DadosProcesso dados(String valor) {
        return new DadosProcesso("Execução fiscal - IPTU", "Contribuinte Exemplo", new BigDecimal(valor), DATA_VALIDA);
    }

    private static void assertCampoInvalido(DadosProcesso dados, String campo) {
        assertThatThrownBy(() -> Processo.novo(NUMERO, dados, CLOCK))
                .isInstanceOf(DadoInvalidoException.class)
                .extracting(e -> ((DadoInvalidoException) e).getCampo())
                .isEqualTo(campo);
    }
}
