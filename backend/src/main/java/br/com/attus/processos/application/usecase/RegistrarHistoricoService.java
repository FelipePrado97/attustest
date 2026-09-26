package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.RegistrarHistoricoUseCase;
import br.com.attus.processos.application.port.out.HistoricoRepositoryPort;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.model.RegistroHistorico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class RegistrarHistoricoService implements RegistrarHistoricoUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegistrarHistoricoService.class);

    private final HistoricoRepositoryPort repositorio;
    private final Clock clock;

    public RegistrarHistoricoService(HistoricoRepositoryPort repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean registrar(ProcessoEvento evento) {
        if (repositorio.existeEvento(evento.eventoId())) {
            log.info("Evento ja processado, ignorando [eventoId={}, processoId={}]", evento.eventoId(), evento.processoId());
            return false;
        }
        repositorio.salvar(new RegistroHistorico(evento.eventoId(), evento.processoId(), evento.tipo(),
                evento.descricao(), evento.ocorridoEm(), Instant.now(clock)));
        log.info("Historico registrado [eventoId={}, processoId={}, tipo={}]", evento.eventoId(), evento.processoId(), evento.tipo());
        return true;
    }
}
