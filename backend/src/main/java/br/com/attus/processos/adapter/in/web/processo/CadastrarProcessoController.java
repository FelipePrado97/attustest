package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.adapter.in.web.processo.dto.ProcessoRequest;
import br.com.attus.processos.adapter.in.web.processo.dto.ProcessoResponse;
import br.com.attus.processos.application.port.in.CadastrarProcessoUseCase;
import br.com.attus.processos.domain.model.DadosProcesso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Tag(name = ProcessoRotas.TAG)
class CadastrarProcessoController {

    private final CadastrarProcessoUseCase cadastrarProcesso;

    CadastrarProcessoController(CadastrarProcessoUseCase cadastrarProcesso) {
        this.cadastrarProcesso = cadastrarProcesso;
    }

    @PostMapping(ProcessoRotas.BASE)
    @Operation(summary = "Cadastra um processo", description = "O processo nasce com status ATIVO.")
    @ApiResponse(responseCode = "201", description = "Processo cadastrado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (inclui CNJ com dígito verificador errado)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Número CNJ já cadastrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ProcessoResponse> cadastrar(@Valid @RequestBody ProcessoRequest request) {
        var processo = cadastrarProcesso.cadastrar(new CadastrarProcessoUseCase.Comando(request.numeroCnj(),
                new DadosProcesso(request.assunto(), request.parteContraria(), request.valorCausa(), request.dataDistribuicao())));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(processo.getId()).toUri();
        return ResponseEntity.created(location).body(ProcessoResponse.de(processo));
    }
}
