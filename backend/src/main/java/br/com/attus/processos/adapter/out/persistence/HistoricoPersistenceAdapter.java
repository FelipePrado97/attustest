package br.com.attus.processos.adapter.out.persistence;

import br.com.attus.processos.application.port.out.HistoricoRepositoryPort;
import br.com.attus.processos.domain.model.RegistroHistorico;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class HistoricoPersistenceAdapter implements HistoricoRepositoryPort {

    private final HistoricoJpaRepository repository;

    HistoricoPersistenceAdapter(HistoricoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existeEvento(UUID eventoId) {
        return repository.existsById(eventoId);
    }

    @Override
    public void salvar(RegistroHistorico r) {
        repository.saveAndFlush(new HistoricoJpaEntity(r.eventoId(), r.processoId(), r.tipo(), r.descricao(),
                r.ocorridoEm(), r.registradoEm()));
    }

    @Override
    public List<RegistroHistorico> listarPorProcesso(UUID processoId) {
        return repository.findByProcessoIdOrderBySequenciaDesc(processoId).stream()
                .map(e -> new RegistroHistorico(e.getEventoId(), e.getProcessoId(), e.getTipo(), e.getDescricao(),
                        e.getOcorridoEm(), e.getRegistradoEm()))
                .toList();
    }
}
