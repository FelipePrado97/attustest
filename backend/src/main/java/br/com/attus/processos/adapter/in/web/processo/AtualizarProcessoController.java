package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.adapter.in.web.processo.dto.AtualizarProcessoRequest;
import br.com.attus.processos.adapter.in.web.processo.dto.ProcessoResponse;
import br.com.attus.processos.application.port.in.AtualizarProcessoUseCase;
import br.com.attus.processos.domain.model.DadosProcesso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = ProcessoRotas.TAG)
class AtualizarProcessoController {

    private final AtualizarProcessoUseCase atualizarProcesso;

    AtualizarProcessoController(AtualizarProcessoUseCase atualizarProcesso) {
        this.atualizarProcesso = atualizarProcesso;
    }

    @PutMapping(ProcessoRotas.POR_ID)
    @Operation(summary = "Atualiza os dados de um processo",
            description = "Exige a versão lida (optimistic lock). Processo arquivado não pode ser editado.")
    @ApiResponse(responseCode = "200", description = "Processo atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Processo não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Versão desatualizada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Processo arquivado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ProcessoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarProcessoRequest request) {
        var processo = atualizarProcesso.atualizar(new AtualizarProcessoUseCase.Comando(id, request.versao(),
                new DadosProcesso(request.assunto(), request.parteContraria(), request.valorCausa(), request.dataDistribuicao())));
        return ProcessoResponse.de(processo);
    }
}
