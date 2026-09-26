package br.com.attus.processos.application.port.in;

import br.com.attus.processos.domain.event.ProcessoEvento;

public interface RegistrarHistoricoUseCase {

    boolean registrar(ProcessoEvento evento);
}
