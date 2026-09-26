package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.in.ExcluirProcessoUseCase;
import br.com.attus.processos.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExcluirProcessoController.class)
class ExcluirProcessoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ExcluirProcessoUseCase excluirProcesso;

    @Test
    void exclusaoRetorna204() throws Exception {
        var id = UUID.randomUUID();

        mvc.perform(delete(ProcessoRotas.BASE + "/" + id)).andExpect(status().isNoContent());

        verify(excluirProcesso).excluir(id);
    }

    @Test
    void processoInexistenteRetorna404() throws Exception {
        var id = UUID.randomUUID();
        doThrow(RecursoNaoEncontradoException.processo(id)).when(excluirProcesso).excluir(id);

        mvc.perform(delete(ProcessoRotas.BASE + "/" + id)).andExpect(status().isNotFound());
    }
}
