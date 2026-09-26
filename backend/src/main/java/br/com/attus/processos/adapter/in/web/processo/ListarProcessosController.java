package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.adapter.in.web.processo.dto.PaginaResponse;
import br.com.attus.processos.adapter.in.web.processo.dto.ProcessoResponse;
import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.application.port.in.ListarProcessosUseCase;
import br.com.attus.processos.domain.model.StatusProcesso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = ProcessoRotas.TAG)
class ListarProcessosController {

    private final ListarProcessosUseCase listarProcessos;

    ListarProcessosController(ListarProcessosUseCase listarProcessos) {
        this.listarProcessos = listarProcessos;
    }

    @GetMapping(ProcessoRotas.BASE)
    @Operation(summary = "Lista processos com filtro e paginação",
            description = "Ordenado pela última atualização (mais recente primeiro).")
    @ApiResponse(responseCode = "200", description = "Página de processos")
    @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos")
    PaginaResponse<ProcessoResponse> listar(
            @Parameter(description = "Busca por número CNJ, assunto ou parte contrária") @RequestParam(required = false) String termo,
            @RequestParam(required = false) StatusProcesso status,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanho) {
        var resultado = listarProcessos.listar(new FiltroProcessos(termo, status), pagina, tamanho);
        return PaginaResponse.de(resultado, ProcessoResponse::de);
    }
}
