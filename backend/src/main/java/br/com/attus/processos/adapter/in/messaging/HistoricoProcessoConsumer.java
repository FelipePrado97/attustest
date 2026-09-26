package br.com.attus.processos.adapter.in.messaging;

import br.com.attus.processos.application.port.in.RegistrarHistoricoUseCase;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.shared.CorrelationId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
class HistoricoProcessoConsumer {

    private static final Logger log = LoggerFactory.getLogger(HistoricoProcessoConsumer.class);

    private final RegistrarHistoricoUseCase registrarHistorico;
    private final ObjectMapper objectMapper;

    HistoricoProcessoConsumer(RegistrarHistoricoUseCase registrarHistorico, ObjectMapper objectMapper) {
        this.registrarHistorico = registrarHistorico;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topico-eventos}", groupId = "${app.kafka.grupo-historico}")
    void consumir(ConsumerRecord<String, String> record) {
        CorrelationId.definir(correlationId(record));
        try {
            log.info("Evento recebido [topico={}, particao={}, offset={}, chave={}]",
                    record.topic(), record.partition(), record.offset(), record.key());
            var evento = objectMapper.readValue(record.value(), ProcessoEvento.class);
            registrarHistorico.registrar(evento);
        } finally {
            CorrelationId.limpar();
        }
    }

    private static String correlationId(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(CorrelationId.HEADER);
        return CorrelationId.aceitarOuGerar(header == null ? null : new String(header.value(), StandardCharsets.UTF_8));
    }
}
