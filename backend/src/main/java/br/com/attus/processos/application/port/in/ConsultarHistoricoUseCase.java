package br.com.attus.processos.application.port.in;

import br.com.attus.processos.domain.model.RegistroHistorico;

import java.util.List;
import java.util.UUID;

public interface ConsultarHistoricoUseCase {

    List<RegistroHistorico> listarPorProcesso(UUID processoId);
}
