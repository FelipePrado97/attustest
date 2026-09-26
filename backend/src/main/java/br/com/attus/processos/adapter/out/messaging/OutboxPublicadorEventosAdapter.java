package br.com.attus.processos.adapter.out.messaging;

import br.com.attus.processos.application.port.out.PublicadorEventosPort;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.shared.CorrelationId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
class OutboxPublicadorEventosAdapter implements PublicadorEventosPort {

    private final OutboxEventoRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    OutboxPublicadorEventosAdapter(OutboxEventoRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(List<ProcessoEvento> eventos) {
        var correlationId = CorrelationId.atual().orElse(null);
        var agora = Instant.now(clock);
        var registros = eventos.stream()
                .map(evento -> new OutboxEventoEntity(evento.eventoId(), evento.processoId(), evento.tipo().name(),
                        objectMapper.writeValueAsString(evento), correlationId, agora))
                .toList();
        repository.saveAll(registros);
    }
}
