package br.com.attus.processos.adapter.out.messaging;

import br.com.attus.processos.shared.CorrelationId;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final Duration TIMEOUT_ENVIO = Duration.ofSeconds(10);
    static final String HEADER_EVENTO_ID = "eventoId";
    static final String HEADER_TIPO = "tipo";

    enum Resultado { PUBLICADO, FALHA_DO_EVENTO, BROKER_INDISPONIVEL }

    private final OutboxEventoRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;
    private final Clock clock;
    private final String topico;

    OutboxRelay(OutboxEventoRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                OutboxProperties properties, Clock clock, @Value("${app.kafka.topico-eventos}") String topico) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.clock = clock;
        this.topico = topico;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    public void publicarPendentes() {
        var agora = Instant.now(clock);
        Set<UUID> processosBloqueados = new HashSet<>();
        for (var evento : repository.findTop500ByPublicadoEmIsNullOrderBySequenciaAsc()) {
            if (processosBloqueados.contains(evento.getAgregadoId())) {
                continue;
            }
            if (evento.aguardandoBackoff(agora)) {
                processosBloqueados.add(evento.getAgregadoId());
                continue;
            }
            switch (publicar(evento)) {
                case PUBLICADO -> { }
                case FALHA_DO_EVENTO -> processosBloqueados.add(evento.getAgregadoId());
                case BROKER_INDISPONIVEL -> {
                    return;
                }
            }
        }
    }

    Resultado publicar(OutboxEventoEntity evento) {
        if (evento.getCorrelationId() != null) {
            CorrelationId.definir(evento.getCorrelationId());
        }
        try {
            var resultado = enviar(evento);
            evento.marcarPublicado(Instant.now(clock));
            repository.save(evento);
            log.info("Evento publicado [eventoId={}, tipo={}, processoId={}, particao={}, offset={}]",
                    evento.getId(), evento.getTipo(), evento.getAgregadoId(),
                    resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
            return Resultado.PUBLICADO;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultado.BROKER_INDISPONIVEL;
        } catch (Exception e) {
            return registrarFalha(evento, e);
        } finally {
            CorrelationId.limpar();
        }
    }

    private SendResult<String, String> enviar(OutboxEventoEntity evento) throws Exception {
        var record = new ProducerRecord<>(topico, evento.getAgregadoId().toString(), evento.getPayload());
        if (evento.getCorrelationId() != null) {
            record.headers().add(CorrelationId.HEADER, evento.getCorrelationId().getBytes(StandardCharsets.UTF_8));
        }
        record.headers().add(HEADER_EVENTO_ID, evento.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add(HEADER_TIPO, evento.getTipo().getBytes(StandardCharsets.UTF_8));
        return kafkaTemplate.send(record).get(TIMEOUT_ENVIO.toMillis(), TimeUnit.MILLISECONDS);
    }

    private Resultado registrarFalha(OutboxEventoEntity evento, Exception e) {
        var erro = descrever(e);
        return brokerIndisponivel(e)
                ? interromperCicloPorBrokerIndisponivel(evento, erro)
                : agendarNovaTentativaDoEvento(evento, erro);
    }

    private Resultado interromperCicloPorBrokerIndisponivel(OutboxEventoEntity evento, String erro) {
        evento.registrarFalha(erro, null);
        repository.save(evento);
        log.warn("Broker indisponivel, ciclo do outbox interrompido [eventoId={}, erro={}]", evento.getId(), erro);
        return Resultado.BROKER_INDISPONIVEL;
    }

    private Resultado agendarNovaTentativaDoEvento(OutboxEventoEntity evento, String erro) {
        var proximaTentativa = Instant.now(clock).plus(properties.backoffApos(evento.getTentativas() + 1));
        evento.registrarFalha(erro, proximaTentativa);
        repository.save(evento);
        log.error("Falha ao publicar evento; processo bloqueado ate nova tentativa [eventoId={}, processoId={}, tentativas={}, proximaTentativa={}, erro={}]",
                evento.getId(), evento.getAgregadoId(), evento.getTentativas(), proximaTentativa, erro);
        return Resultado.FALHA_DO_EVENTO;
    }

    private static boolean brokerIndisponivel(Throwable e) {
        for (var causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof TimeoutException || causa instanceof RetriableException) {
                return true;
            }
        }
        return false;
    }

    private static String descrever(Exception e) {
        var causa = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
