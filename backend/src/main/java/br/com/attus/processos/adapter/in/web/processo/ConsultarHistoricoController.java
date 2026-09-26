package br.com.attus.processos.adapter.in.web.processo;

import br.com.attus.processos.adapter.in.web.processo.dto.HistoricoResponse;
import br.com.attus.processos.application.port.in.ConsultarHistoricoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = ProcessoRotas.TAG)
class ConsultarHistoricoController {

    private final ConsultarHistoricoUseCase consultarHistorico;

    ConsultarHistoricoController(ConsultarHistoricoUseCase consultarHistorico) {
        this.consultarHistorico = consultarHistorico;
    }

    @GetMapping(ProcessoRotas.HISTORICO)
    @Operation(summary = "Linha do tempo do processo",
            description = "Construída de forma assíncrona via Kafka: pode levar alguns instantes para refletir a última alteração. "
                    + "Mantida mesmo após a exclusão do processo (auditoria).")
    @ApiResponse(responseCode = "200", description = "Eventos do mais recente para o mais antigo")
    List<HistoricoResponse> historico(@PathVariable UUID id) {
        return consultarHistorico.listarPorProcesso(id).stream().map(HistoricoResponse::de).toList();
    }
}
