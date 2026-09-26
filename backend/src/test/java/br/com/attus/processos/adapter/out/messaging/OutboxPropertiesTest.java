package br.com.attus.processos.adapter.out.messaging;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPropertiesTest {

    private final OutboxProperties properties =
            new OutboxProperties(Duration.ofSeconds(1), Duration.ofMinutes(5), Duration.ofDays(7));

    @ParameterizedTest(name = "tentativa {0} -> {1}s")
    @CsvSource({
            "0,   1",
            "1,   1",
            "2,   2",
            "3,   4",
            "9,   256",
            "10,  300",
            "1000, 300"
    })
    void backoffExponencialComTeto(int tentativas, long segundosEsperados) {
        assertThat(properties.backoffApos(tentativas)).isEqualTo(Duration.ofSeconds(segundosEsperados));
    }
}
