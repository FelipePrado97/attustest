package br.com.attus.processos.adapter.in.web.processo.dto;

import br.com.attus.processos.domain.model.Processo;
import br.com.attus.processos.domain.model.StatusProcesso;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProcessoResponse(
        UUID id,
        String numeroCnj,
        String assunto,
        String parteContraria,
        BigDecimal valorCausa,
        LocalDate dataDistribuicao,
        StatusProcesso status,
        @Schema(description = "Status para os quais o processo pode transicionar a partir do atual")
        Set<StatusProcesso> transicoesPermitidas,
        Long versao,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static ProcessoResponse de(Processo p) {
        return new ProcessoResponse(p.getId(), p.getNumero().valor(), p.getAssunto(), p.getParteContraria(),
                p.getValorCausa(), p.getDataDistribuicao(), p.getStatus(), p.getStatus().transicoesPermitidas(),
                p.getVersao(), p.getCriadoEm(), p.getAtualizadoEm());
    }
}
