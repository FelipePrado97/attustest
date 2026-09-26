package br.com.attus.processos.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusProcessoTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "ATIVO,     SUSPENSO,  true",
            "ATIVO,     ARQUIVADO, true",
            "ATIVO,     ATIVO,     false",
            "SUSPENSO,  ATIVO,     true",
            "SUSPENSO,  ARQUIVADO, true",
            "ARQUIVADO, ATIVO,     true",
            "ARQUIVADO, SUSPENSO,  false",
    })
    void transicoes(StatusProcesso origem, StatusProcesso destino, boolean permitida) {
        assertThat(origem.podeTransicionarPara(destino)).isEqualTo(permitida);
    }
}
