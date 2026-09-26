package br.com.attus.processos.adapter.out.messaging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxMetricasTest {

    @Mock
    private OutboxEventoRepository repository;

    @Test
    void expoePendentesEComFalha() {
        when(repository.countByPublicadoEmIsNull()).thenReturn(7L);
        when(repository.countByPublicadoEmIsNullAndTentativasGreaterThan(0)).thenReturn(2L);
        var registry = new SimpleMeterRegistry();

        new OutboxMetricas(repository).bindTo(registry);

        assertThat(registry.get("outbox.eventos.pendentes").gauge().value()).isEqualTo(7.0);
        assertThat(registry.get("outbox.eventos.com.falha").gauge().value()).isEqualTo(2.0);
    }
}
