package br.com.attus.processos.adapter.out.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.outbox")
public record OutboxProperties(
        @DefaultValue("1s") Duration backoffInicial,
        @DefaultValue("5m") Duration backoffMaximo,
        @DefaultValue("7d") Duration retencao) {

    private static final int EXPOENTE_MAXIMO_SEM_OVERFLOW = 30;

    Duration backoffApos(int tentativas) {
        var expoente = Math.min(Math.max(tentativas - 1, 0), EXPOENTE_MAXIMO_SEM_OVERFLOW);
        var espera = backoffInicial.multipliedBy(1L << expoente);
        return espera.compareTo(backoffMaximo) > 0 ? backoffMaximo : espera;
    }
}
