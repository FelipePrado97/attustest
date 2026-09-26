package br.com.attus.processos.adapter.in.messaging;

import br.com.attus.processos.application.port.in.RegistrarHistoricoUseCase;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.shared.CorrelationId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HistoricoProcessoConsumerTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private RegistrarHistoricoUseCase registrarHistorico;

    private HistoricoProcessoConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new HistoricoProcessoConsumer(registrarHistorico, jsonMapper);
    }

    @Test
    void desserializaERegistraOEvento() {
        var evento = evento();

        consumer.consumir(record(jsonMapper.writeValueAsString(evento), null));

        verify(registrarHistorico).registrar(evento);
    }

    @Test
    void usaOCorrelationIdDoHeaderDuranteOProcessamentoELimpaDepois() {
        var durante = new AtomicReference<String>();
        doAnswer(inv -> {
            durante.set(MDC.get(CorrelationId.MDC_KEY));
            return true;
        }).when(registrarHistorico).registrar(any());

        consumer.consumir(record(jsonMapper.writeValueAsString(evento()), "cid-kafka-0001"));

        assertThat(durante.get()).isEqualTo("cid-kafka-0001");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void semHeaderGeraUmCorrelationIdNovo() {
        var durante = new AtomicReference<String>();
        doAnswer(inv -> {
            durante.set(MDC.get(CorrelationId.MDC_KEY));
            return true;
        }).when(registrarHistorico).registrar(any());

        consumer.consumir(record(jsonMapper.writeValueAsString(evento()), null));

        assertThat(durante.get()).matches("[0-9a-f-]{36}");
    }

    @Test
    void jsonInvalidoPropagaAExcecaoSemRegistrar() {
        assertThatThrownBy(() -> consumer.consumir(record("{ quebrado", null))).isInstanceOf(JacksonException.class);

        verifyNoInteractions(registrarHistorico);
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void falhaAoRegistrarPropagaParaRetry() {
        doThrow(new IllegalStateException("banco fora")).when(registrarHistorico).registrar(any());

        assertThatThrownBy(() -> consumer.consumir(record(jsonMapper.writeValueAsString(evento()), null)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ProcessoEvento evento() {
        return new ProcessoEvento(UUID.randomUUID(), TipoEvento.CADASTRADO, UUID.randomUUID(),
                "0001234-71.2024.8.26.0100", "Processo cadastrado", Instant.parse("2025-06-10T15:00:00Z"));
    }

    private static ConsumerRecord<String, String> record(String valor, String correlationId) {
        var record = new ConsumerRecord<>("processos.eventos", 0, 0L, "chave", valor);
        if (correlationId != null) {
            record.headers().add(CorrelationId.HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }
}
