package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.adapter.in.web.processo.dto.ProcessoResponse;
import br.com.attus.processos.application.port.in.BuscarProcessoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = ProcessoRotas.TAG)
class BuscarProcessoController {

    private final BuscarProcessoUseCase buscarProcesso;

    BuscarProcessoController(BuscarProcessoUseCase buscarProcesso) {
        this.buscarProcesso = buscarProcesso;
    }

    @GetMapping(ProcessoRotas.POR_ID)
    @Operation(summary = "Busca um processo pelo id")
    @ApiResponse(responseCode = "200", description = "Processo encontrado")
    @ApiResponse(responseCode = "404", description = "Processo não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ProcessoResponse buscar(@PathVariable UUID id) {
        return ProcessoResponse.de(buscarProcesso.buscarPorId(id));
    }
}
