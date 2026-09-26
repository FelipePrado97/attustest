package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.in.BuscarProcessoUseCase;
import br.com.attus.processos.domain.exception.RecursoNaoEncontradoException;
import br.com.attus.processos.shared.CorrelationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.CNJ;
import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuscarProcessoController.class)
class BuscarProcessoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BuscarProcessoUseCase buscarProcesso;

    @Test
    void retornaProcesso() throws Exception {
        var processo = persistido();
        when(buscarProcesso.buscarPorId(processo.getId())).thenReturn(processo);

        mvc.perform(get(ProcessoRotas.BASE + "/" + processo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(processo.getId().toString()))
                .andExpect(jsonPath("$.numeroCnj").value(CNJ))
                .andExpect(jsonPath("$.valorCausa").value(1500.00));
    }

    @Test
    void processoInexistenteRetorna404() throws Exception {
        var id = naoEncontrado();

        mvc.perform(get(ProcessoRotas.BASE + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void idMalformadoRetorna400() throws Exception {
        mvc.perform(get(ProcessoRotas.BASE + "/nao-e-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void propagaCorrelationIdRecebido() throws Exception {
        var id = naoEncontrado();

        mvc.perform(get(ProcessoRotas.BASE + "/" + id).header(CorrelationId.HEADER, "teste-correlacao-123"))
                .andExpect(header().string(CorrelationId.HEADER, "teste-correlacao-123"))
                .andExpect(jsonPath("$.correlationId").value("teste-correlacao-123"));
    }

    @Test
    void descartaCorrelationIdComCaracteresInseguros() throws Exception {
        var id = naoEncontrado();

        mvc.perform(get(ProcessoRotas.BASE + "/" + id).header(CorrelationId.HEADER, "x\nFAKE LOG LINE"))
                .andExpect(header().string(CorrelationId.HEADER, matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void erroInesperadoRetorna500GenericoSemVazarDetalhesInternos() throws Exception {
        var id = UUID.randomUUID();
        when(buscarProcesso.buscarPorId(id)).thenThrow(new IllegalStateException("senha=segredo; host=db-interno"));

        mvc.perform(get(ProcessoRotas.BASE + "/" + id).header(CorrelationId.HEADER, "cid-erro-interno-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Erro interno. Informe o correlationId ao suporte."))
                .andExpect(jsonPath("$.correlationId").value("cid-erro-interno-1"))
                .andExpect(content().string(not(containsString("segredo"))));
    }

    @Test
    void errosDoProprioSpringTambemTrazemCorrelationId() throws Exception {
        mvc.perform(get("/api/v1/rota-inexistente").header(CorrelationId.HEADER, "cid-rota-404-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.correlationId").value("cid-rota-404-01"));

        mvc.perform(delete(ProcessoRotas.BASE + "/" + UUID.randomUUID()).header(CorrelationId.HEADER, "cid-metodo-405"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.correlationId").value("cid-metodo-405"));
    }

    private UUID naoEncontrado() {
        var id = UUID.randomUUID();
        when(buscarProcesso.buscarPorId(id)).thenThrow(RecursoNaoEncontradoException.processo(id));
        return id;
    }
}
