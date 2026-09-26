package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.adapter.in.web.processo.dto.AlterarStatusRequest;
import br.com.attus.processos.adapter.in.web.processo.dto.ProcessoResponse;
import br.com.attus.processos.application.port.in.AlterarStatusProcessoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = ProcessoRotas.TAG)
class AlterarStatusProcessoController {

    private final AlterarStatusProcessoUseCase alterarStatus;

    AlterarStatusProcessoController(AlterarStatusProcessoUseCase alterarStatus) {
        this.alterarStatus = alterarStatus;
    }

    @PatchMapping(ProcessoRotas.STATUS)
    @Operation(summary = "Altera o status do processo",
            description = "Transições: ATIVO → SUSPENSO|ARQUIVADO; SUSPENSO → ATIVO|ARQUIVADO; ARQUIVADO → ATIVO.")
    @ApiResponse(responseCode = "200", description = "Status alterado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Processo não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Versão desatualizada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Transição não permitida", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ProcessoResponse alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlterarStatusRequest request) {
        return ProcessoResponse.de(alterarStatus.alterarStatus(
                new AlterarStatusProcessoUseCase.Comando(id, request.versao(), request.status())));
    }
}
