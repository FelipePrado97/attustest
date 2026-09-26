package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.in.AlterarStatusProcessoUseCase;
import br.com.attus.processos.domain.exception.TransicaoStatusInvalidaException;
import br.com.attus.processos.domain.model.StatusProcesso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlterarStatusProcessoController.class)
class AlterarStatusProcessoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AlterarStatusProcessoUseCase alterarStatus;

    @Test
    void retornaProcessoComNovoStatusETransicoesPermitidas() throws Exception {
        when(alterarStatus.alterarStatus(any())).thenReturn(persistido(StatusProcesso.SUSPENSO, 1L));

        mvc.perform(patch(url()).contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"SUSPENSO\", \"versao\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENSO"))
                .andExpect(jsonPath("$.versao").value(1));
    }

    @Test
    void transicaoInvalidaRetorna422() throws Exception {
        when(alterarStatus.alterarStatus(any()))
                .thenThrow(new TransicaoStatusInvalidaException(StatusProcesso.ARQUIVADO, StatusProcesso.SUSPENSO));

        mvc.perform(patch(url()).contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"SUSPENSO\", \"versao\": 0}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void statusDesconhecidoRetorna400() throws Exception {
        mvc.perform(patch(url()).contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"ENCERRADO\", \"versao\": 0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(alterarStatus);
    }

    private static String url() {
        return ProcessoRotas.BASE + "/" + UUID.randomUUID() + "/status";
    }
}
