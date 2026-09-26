package br.com.attus.processos.application.port.out;

import br.com.attus.processos.domain.model.RegistroHistorico;

import java.util.List;
import java.util.UUID;

public interface HistoricoRepositoryPort {

    boolean existeEvento(UUID eventoId);

    void salvar(RegistroHistorico registro);

    List<RegistroHistorico> listarPorProcesso(UUID processoId);
}
