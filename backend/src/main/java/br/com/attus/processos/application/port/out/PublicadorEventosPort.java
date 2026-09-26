package br.com.attus.processos.application.port.out;

import br.com.attus.processos.domain.event.ProcessoEvento;

import java.util.List;

public interface PublicadorEventosPort {

    void publicar(List<ProcessoEvento> eventos);
}
