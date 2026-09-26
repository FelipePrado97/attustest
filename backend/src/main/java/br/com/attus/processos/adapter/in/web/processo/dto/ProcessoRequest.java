package br.com.attus.processos.adapter.in.web.processo.dto;

import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.Processo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para cadastro de um processo")
public record ProcessoRequest(

        @Schema(description = "Número único CNJ, com ou sem máscara", example = "0001234-71.2024.8.26.0100")
        @NotBlank @Size(max = NumeroCnj.TAMANHO_FORMATADO)
        String numeroCnj,

        @Schema(description = "Assunto / objeto da ação", example = "Execução fiscal - IPTU 2021")
        @NotBlank @Size(max = Processo.TAMANHO_MAXIMO_ASSUNTO)
        String assunto,

        @Schema(description = "Nome da parte contrária", example = "Empresa Exemplo Ltda")
        @NotBlank @Size(max = Processo.TAMANHO_MAXIMO_PARTE)
        String parteContraria,

        @Schema(description = "Valor da causa em reais", example = "15230.50")
        @NotNull @DecimalMin("0.00") @Digits(integer = Processo.DIGITOS_INTEIROS_VALOR, fraction = Processo.CASAS_DECIMAIS_VALOR)
        BigDecimal valorCausa,

        @Schema(description = DESCRICAO_DATA_DISTRIBUICAO, example = "2024-03-15")
        @NotNull
        LocalDate dataDistribuicao) {

    public static final String DESCRICAO_DATA_DISTRIBUICAO =
            "Data de distribuição: entre 1º de janeiro do ano de ajuizamento (segmento AAAA do número CNJ, "
                    + "nunca antes de 1900) e hoje";
}
