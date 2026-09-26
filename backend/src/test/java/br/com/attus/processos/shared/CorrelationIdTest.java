package br.com.attus.processos.shared;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdTest {

    @AfterEach
    void limpar() {
        CorrelationId.limpar();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd1234", "3f2b8c1e-9a7d-4c1b-8e2f-0a1b2c3d4e5f", "req-2025-06-10-0001"})
    void aceitaValoresSeguros(String valor) {
        assertThat(CorrelationId.aceitarOuGerar(valor)).isEqualTo(valor);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"curto", "com espaço", "quebra\nde linha", "x;DROP", "muito-longo-" + "0123456789012345678901234567890123456789012345678901234"})
    void geraNovoParaValoresAusentesOuInseguros(String valor) {
        assertThat(CorrelationId.aceitarOuGerar(valor)).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void cicloDeVidaNoMdc() {
        assertThat(CorrelationId.atual()).isEmpty();

        CorrelationId.definir("abcd1234");
        assertThat(CorrelationId.atual()).contains("abcd1234");

        CorrelationId.limpar();
        assertThat(CorrelationId.atual()).isEmpty();
    }
}
