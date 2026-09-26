package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.in.AtualizarProcessoUseCase;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.exception.ProcessoArquivadoException;
import br.com.attus.processos.domain.model.NumeroCnj;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.CNJ;
import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AtualizarProcessoController.class)
class AtualizarProcessoControllerTest {

    private static final String CORPO = """
            {"assunto": "Novo assunto", "parteContraria": "Parte", "valorCausa": 10.5,
             "dataDistribuicao": "2024-01-01", "versao": 3}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AtualizarProcessoUseCase atualizarProcesso;

    @Test
    void repassaIdEVersaoParaOCasoDeUso() throws Exception {
        var id = UUID.randomUUID();
        when(atualizarProcesso.atualizar(any())).thenReturn(persistido());

        mvc.perform(put(ProcessoRotas.BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());

        var comando = ArgumentCaptor.forClass(AtualizarProcessoUseCase.Comando.class);
        verify(atualizarProcesso).atualizar(comando.capture());
        assertThat(comando.getValue().id()).isEqualTo(id);
        assertThat(comando.getValue().versao()).isEqualTo(3L);
        assertThat(comando.getValue().dados().assunto()).isEqualTo("Novo assunto");
    }

    @Test
    void semVersaoRetorna400() throws Exception {
        mvc.perform(put(ProcessoRotas.BASE + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assunto": "A", "parteContraria": "B", "valorCausa": 1, "dataDistribuicao": "2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("versao"));

        verifyNoInteractions(atualizarProcesso);
    }

    @Test
    void versaoDesatualizadaRetorna409() throws Exception {
        when(atualizarProcesso.atualizar(any())).thenThrow(ConflitoException.versaoDesatualizada());

        mvc.perform(put(ProcessoRotas.BASE + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isConflict());
    }

    @Test
    void processoArquivadoRetorna422() throws Exception {
        when(atualizarProcesso.atualizar(any())).thenThrow(new ProcessoArquivadoException(NumeroCnj.of(CNJ)));

        mvc.perform(put(ProcessoRotas.BASE + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnprocessableContent());
    }
}
