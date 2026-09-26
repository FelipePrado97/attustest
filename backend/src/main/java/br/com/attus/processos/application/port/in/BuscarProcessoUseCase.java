package br.com.attus.processos.application.port.in;

import br.com.attus.processos.domain.model.Processo;

import java.util.UUID;

public interface BuscarProcessoUseCase {

    Processo buscarPorId(UUID id);
}
