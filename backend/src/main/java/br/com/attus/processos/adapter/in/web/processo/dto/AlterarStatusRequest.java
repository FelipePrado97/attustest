package br.com.attus.processos.adapter.in.web.processo.dto;

import br.com.attus.processos.domain.model.StatusProcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(

        @Schema(description = "Novo status", example = "SUSPENSO")
        @NotNull
        StatusProcesso status,

        @Schema(description = "Versão lida pelo cliente (controle de concorrência otimista)", example = "0")
        @NotNull
        Long versao) {
}
