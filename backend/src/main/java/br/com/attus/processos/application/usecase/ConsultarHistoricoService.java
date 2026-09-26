package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.ConsultarHistoricoUseCase;
import br.com.attus.processos.application.port.out.HistoricoRepositoryPort;
import br.com.attus.processos.domain.model.RegistroHistorico;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class ConsultarHistoricoService implements ConsultarHistoricoUseCase {

    private final HistoricoRepositoryPort repositorio;

    public ConsultarHistoricoService(HistoricoRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroHistorico> listarPorProcesso(UUID processoId) {
        return repositorio.listarPorProcesso(processoId);
    }
}
