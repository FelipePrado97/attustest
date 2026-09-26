package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.application.port.Pagina;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.domain.model.Processo;
import br.com.attus.processos.domain.model.StatusProcesso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarProcessosServiceTest {

    @Mock
    private ProcessoRepositoryPort repositorio;

    @Test
    void repassaFiltroEPaginacao() {
        var filtro = new FiltroProcessos("iptu", StatusProcesso.ATIVO);
        var pagina = new Pagina<Processo>(List.of(), 2, 20, 0);
        when(repositorio.buscar(filtro, 2, 20)).thenReturn(pagina);

        assertThat(new ListarProcessosService(repositorio).listar(filtro, 2, 20)).isSameAs(pagina);
    }

    @Test
    void filtroNuloViraFiltroVazio() {
        new ListarProcessosService(repositorio).listar(null, 0, 10);

        verify(repositorio).buscar(FiltroProcessos.vazio(), 0, 10);
    }
}
