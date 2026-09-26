package br.com.attus.processos.application.port;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaTest {

    @ParameterizedTest(name = "{0} itens / tamanho {1} = {2} páginas")
    @CsvSource({"0, 10, 0", "1, 10, 1", "10, 10, 1", "11, 10, 2", "5, 0, 0"})
    void totalDePaginas(long total, int tamanho, int esperado) {
        assertThat(new Pagina<>(List.of(), 0, tamanho, total).totalPaginas()).isEqualTo(esperado);
    }

    @Test
    void mapPreservaMetadados() {
        var mapeada = new Pagina<>(List.of(1, 2), 3, 2, 8).map(n -> "#" + n);

        assertThat(mapeada).isEqualTo(new Pagina<>(List.of("#1", "#2"), 3, 2, 8));
    }
}
