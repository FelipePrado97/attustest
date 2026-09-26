package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.in.ConsultarHistoricoUseCase;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.model.RegistroHistorico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.AGORA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultarHistoricoController.class)
class ConsultarHistoricoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarHistoricoUseCase consultarHistorico;

    @Test
    void retornaLinhaDoTempo() throws Exception {
        var processoId = UUID.randomUUID();
        when(consultarHistorico.listarPorProcesso(processoId)).thenReturn(List.of(
                new RegistroHistorico(UUID.randomUUID(), processoId, TipoEvento.STATUS_ALTERADO,
                        "Status alterado de ATIVO para SUSPENSO", AGORA, AGORA),
                new RegistroHistorico(UUID.randomUUID(), processoId, TipoEvento.CADASTRADO,
                        "Processo cadastrado", AGORA, AGORA)));

        mvc.perform(get(ProcessoRotas.BASE + "/" + processoId + "/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("STATUS_ALTERADO"))
                .andExpect(jsonPath("$[1].descricao").value("Processo cadastrado"));
    }

    @Test
    void processoSemEventosRetornaListaVazia() throws Exception {
        var processoId = UUID.randomUUID();
        when(consultarHistorico.listarPorProcesso(processoId)).thenReturn(List.of());

        mvc.perform(get(ProcessoRotas.BASE + "/" + processoId + "/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
