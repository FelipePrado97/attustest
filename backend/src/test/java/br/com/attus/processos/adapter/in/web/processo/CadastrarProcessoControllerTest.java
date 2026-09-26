package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.in.CadastrarProcessoUseCase;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.exception.DadoInvalidoException;
import br.com.attus.processos.domain.model.NumeroCnj;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.attus.processos.support.ProcessoFixture.CNJ;
import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CadastrarProcessoController.class)
class CadastrarProcessoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CadastrarProcessoUseCase cadastrarProcesso;

    @Test
    void cadastroValidoRetorna201ComLocation() throws Exception {
        var processo = persistido();
        when(cadastrarProcesso.cadastrar(any())).thenReturn(processo);

        mvc.perform(post(ProcessoRotas.BASE).contentType(MediaType.APPLICATION_JSON).content(corpoValido()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + ProcessoRotas.BASE + "/" + processo.getId()))
                .andExpect(jsonPath("$.numeroCnj").value(CNJ))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.transicoesPermitidas", containsInAnyOrder("SUSPENSO", "ARQUIVADO")));
    }

    @Test
    void camposInvalidosRetornam400ComListaDeErrosSemChamarOCasoDeUso() throws Exception {
        var corpo = """
                {"numeroCnj": "", "assunto": "", "parteContraria": "Fulano", "valorCausa": -1, "dataDistribuicao": null}
                """;

        mvc.perform(post(ProcessoRotas.BASE).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("numeroCnj")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("assunto")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("valorCausa")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("dataDistribuicao")))
                .andExpect(jsonPath("$.correlationId", notNullValue()));

        verifyNoInteractions(cadastrarProcesso);
    }

    @Test
    void regraDoDominioViolada_retorna400ApontandoOCampo() throws Exception {
        when(cadastrarProcesso.cadastrar(any()))
                .thenThrow(new DadoInvalidoException("numeroCnj", "Número CNJ com dígito verificador inválido"));

        mvc.perform(post(ProcessoRotas.BASE).contentType(MediaType.APPLICATION_JSON).content(corpoValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("numeroCnj"));
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mvc.perform(post(ProcessoRotas.BASE).contentType(MediaType.APPLICATION_JSON).content("{ invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    void numeroDuplicadoRetorna409() throws Exception {
        when(cadastrarProcesso.cadastrar(any())).thenThrow(ConflitoException.numeroDuplicado(NumeroCnj.of(CNJ)));

        mvc.perform(post(ProcessoRotas.BASE).contentType(MediaType.APPLICATION_JSON).content(corpoValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe processo cadastrado com o número " + CNJ));
    }

    private static String corpoValido() {
        return """
                {"numeroCnj": "%s", "assunto": "Execução fiscal", "parteContraria": "Contribuinte",
                 "valorCausa": 1500.00, "dataDistribuicao": "2024-03-15"}
                """.formatted(CNJ);
    }
}
