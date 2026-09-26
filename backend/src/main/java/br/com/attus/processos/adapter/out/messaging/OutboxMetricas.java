package br.com.attus.processos.adapter.out.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
class OutboxMetricas implements MeterBinder {

    private final OutboxEventoRepository repository;

    OutboxMetricas(OutboxEventoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("outbox.eventos.pendentes", repository, OutboxEventoRepository::countByPublicadoEmIsNull)
                .description("Eventos gravados no outbox e ainda não publicados no Kafka")
                .register(registry);
        Gauge.builder("outbox.eventos.com.falha", repository, r -> r.countByPublicadoEmIsNullAndTentativasGreaterThan(0))
                .description("Eventos pendentes que já falharam ao menos uma vez")
                .register(registry);
    }
}
