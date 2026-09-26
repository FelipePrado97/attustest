package br.com.attus.processos.adapter.out.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
class OutboxLimpeza {

    private static final Logger log = LoggerFactory.getLogger(OutboxLimpeza.class);

    private final OutboxEventoRepository repository;
    private final OutboxProperties properties;
    private final Clock clock;

    OutboxLimpeza(OutboxEventoRepository repository, OutboxProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.outbox.limpeza-cron:0 0 3 * * *}")
    @Transactional
    public int expurgarPublicados() {
        var limite = Instant.now(clock).minus(properties.retencao());
        var removidos = repository.excluirPublicadosAntesDe(limite);
        log.info("Limpeza do outbox concluida [removidos={}, publicadosAntesDe={}]", removidos, limite);
        return removidos;
    }
}
