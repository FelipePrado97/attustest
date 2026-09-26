package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.application.port.Pagina;
import br.com.attus.processos.application.port.in.ListarProcessosUseCase;
import br.com.attus.processos.domain.model.StatusProcesso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListarProcessosController.class)
class ListarProcessosControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListarProcessosUseCase listarProcessos;

    @Test
    void retornaPaginaComMetadados() throws Exception {
        when(listarProcessos.listar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(persistido(), persistido()), 0, 2, 5));

        mvc.perform(get(ProcessoRotas.BASE).param("tamanho", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.totalItens").value(5))
                .andExpect(jsonPath("$.totalPaginas").value(3));
    }

    @Test
    void repassaFiltrosEPaginacao() throws Exception {
        when(listarProcessos.listar(any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 1, 20, 0));

        mvc.perform(get(ProcessoRotas.BASE).param("termo", "iptu").param("status", "SUSPENSO")
                        .param("pagina", "1").param("tamanho", "20"))
                .andExpect(status().isOk());

        verify(listarProcessos).listar(new FiltroProcessos("iptu", StatusProcesso.SUSPENSO), 1, 20);
    }

    @Test
    void tamanhoDePaginaAcimaDoLimiteRetorna400() throws Exception {
        mvc.perform(get(ProcessoRotas.BASE).param("tamanho", "500")).andExpect(status().isBadRequest());

        verifyNoInteractions(listarProcessos);
    }
}
