package br.com.attus.processos.domain.model;

import br.com.attus.processos.domain.exception.DadoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumeroCnjTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "0001234-71.2024.8.26.0100",
            "5001234-17.2023.4.04.7100",
            "1000000-27.2025.5.02.0001"
    })
    void aceitaNumerosComDigitoVerificadorValido(String numero) {
        assertThat(NumeroCnj.of(numero).valor()).isEqualTo(numero);
    }

    @Test
    void normalizaEntradaSemMascara() {
        assertThat(NumeroCnj.of("00012347120248260100").valor()).isEqualTo("0001234-71.2024.8.26.0100");
    }

    @Test
    void numerosIguaisComFormatosDiferentesSaoIguais() {
        assertThat(NumeroCnj.of(" 0001234-71.2024.8.26.0100 ")).isEqualTo(NumeroCnj.of("00012347120248260100"));
    }

    @Test
    void rejeitaDigitoVerificadorInvalido() {
        assertThatThrownBy(() -> NumeroCnj.of("0001234-72.2024.8.26.0100"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("dígito verificador");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "0001234-71.2024.8.26.01000", "abcdefg-hi.jklm.n.op.qrst"})
    void rejeitaQuantidadeDeDigitosErrada(String numero) {
        assertThatThrownBy(() -> NumeroCnj.of(numero))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("20 dígitos");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejeitaVazio(String numero) {
        assertThatThrownBy(() -> NumeroCnj.of(numero))
                .isInstanceOf(DadoInvalidoException.class)
                .extracting(e -> ((DadoInvalidoException) e).getCampo())
                .isEqualTo("numeroCnj");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"00012347120248260100", "0001234-71.2024.8.26.0100 ", "qualquer coisa"})
    void construtorSoAceitaAFormaFormatada(String valor) {
        assertThatThrownBy(() -> new NumeroCnj(valor))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("formato");
    }

    @Test
    void construtorTambemValidaODigitoVerificador() {
        assertThatThrownBy(() -> new NumeroCnj("0001234-72.2024.8.26.0100"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("dígito verificador");
    }

    @Test
    void anoDeAjuizamentoEhOSegmentoAAAA() {
        assertThat(NumeroCnj.of("0001234-71.2024.8.26.0100").anoAjuizamento()).isEqualTo(2024);
        assertThat(NumeroCnj.of("5001234-17.2023.4.04.7100").anoAjuizamento()).isEqualTo(2023);
    }

    @Test
    void somenteDigitosRemoveAMascara() {
        assertThat(NumeroCnj.somenteDigitos("0001234-71.2024.8.26.0100")).isEqualTo("00012347120248260100");
    }

    @Test
    void toStringDevolveAFormaFormatada() {
        assertThat(NumeroCnj.of("00012347120248260100")).hasToString("0001234-71.2024.8.26.0100");
    }
}
