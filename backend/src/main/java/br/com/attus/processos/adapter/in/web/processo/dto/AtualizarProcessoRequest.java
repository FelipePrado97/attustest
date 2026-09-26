package br.com.attus.processos.adapter.in.web.processo.dto;

import br.com.attus.processos.domain.model.Processo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados editáveis de um processo. O número CNJ é imutável.")
public record AtualizarProcessoRequest(

        @NotBlank @Size(max = Processo.TAMANHO_MAXIMO_ASSUNTO)
        String assunto,

        @NotBlank @Size(max = Processo.TAMANHO_MAXIMO_PARTE)
        String parteContraria,

        @NotNull @DecimalMin("0.00") @Digits(integer = Processo.DIGITOS_INTEIROS_VALOR, fraction = Processo.CASAS_DECIMAIS_VALOR)
        BigDecimal valorCausa,

        @Schema(description = ProcessoRequest.DESCRICAO_DATA_DISTRIBUICAO, example = "2024-03-15")
        @NotNull
        LocalDate dataDistribuicao,

        @Schema(description = "Versão lida pelo cliente (controle de concorrência otimista)", example = "0")
        @NotNull
        Long versao) {
}
