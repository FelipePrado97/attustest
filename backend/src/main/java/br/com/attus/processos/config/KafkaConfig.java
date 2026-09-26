package br.com.attus.processos.config;

import br.com.attus.processos.domain.exception.DominioException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaConfig {

    public static final String SUFIXO_DLT = ".DLT";

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    NewTopic topicoEventosProcesso(@Value("${app.kafka.topico-eventos}") String topico,
                                   @Value("${app.kafka.particoes:3}") int particoes) {
        return TopicBuilder.name(topico).partitions(particoes).replicas(1).build();
    }

    @Bean
    NewTopic topicoEventosProcessoDlt(@Value("${app.kafka.topico-eventos}") String topico,
                                      @Value("${app.kafka.particoes:3}") int particoes) {
        return TopicBuilder.name(topico + SUFIXO_DLT).partitions(particoes).replicas(1).build();
    }

    @Bean
    ProducerListener<Object, Object> kafkaProducerListenerSemDadosPessoaisNoLog() {
        var listener = new LoggingProducerListener<Object, Object>();
        listener.setIncludeContents(false);
        return listener;
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
                                          @Value("${app.kafka.retry.intervalo-inicial-ms:500}") long intervaloInicial,
                                          @Value("${app.kafka.retry.max-tentativas:4}") int maxTentativas) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, KafkaConfig::mesmaParticaoNoDlt);

        var backOff = new ExponentialBackOff(intervaloInicial, 2.0);
        backOff.setMaxAttempts(maxTentativas);

        var handler = new DefaultErrorHandler((record, ex) -> {
            log.error("Mensagem enviada para DLT apos esgotar tentativas [topico={}, particao={}, offset={}, chave={}, erro={}]",
                    record.topic(), record.partition(), record.offset(), record.key(), causaRaiz(ex));
            recoverer.accept(record, ex);
        }, backOff);
        handler.addNotRetryableExceptions(JacksonException.class, DominioException.class);
        handler.setRetryListeners((record, ex, tentativa) ->
                log.warn("Falha ao processar mensagem, tentativa {} [topico={}, offset={}, erro={}]",
                        tentativa, record.topic(), record.offset(), causaRaiz(ex)));
        return handler;
    }

    private static TopicPartition mesmaParticaoNoDlt(ConsumerRecord<?, ?> record, Exception erro) {
        return new TopicPartition(record.topic() + SUFIXO_DLT, record.partition());
    }

    private static String causaRaiz(Exception ex) {
        var causa = NestedExceptionUtils.getMostSpecificCause(ex);
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
