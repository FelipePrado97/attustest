package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.application.port.in.ExcluirProcessoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = ProcessoRotas.TAG)
class ExcluirProcessoController {

    private final ExcluirProcessoUseCase excluirProcesso;

    ExcluirProcessoController(ExcluirProcessoUseCase excluirProcesso) {
        this.excluirProcesso = excluirProcesso;
    }

    @DeleteMapping(ProcessoRotas.POR_ID)
    @Operation(summary = "Exclui um processo", description = "O histórico é mantido como trilha de auditoria.")
    @ApiResponse(responseCode = "204", description = "Processo excluído")
    @ApiResponse(responseCode = "404", description = "Processo não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Processo alterado por outro usuário durante a exclusão", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> excluir(@PathVariable UUID id) {
        excluirProcesso.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
