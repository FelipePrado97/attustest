package br.com.attus.processos.shared;

import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern FORMATO_SEGURO_PARA_LOG = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private CorrelationId() {
    }

    public static String aceitarOuGerar(String recebido) {
        if (recebido != null && FORMATO_SEGURO_PARA_LOG.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }

    public static Optional<String> atual() {
        return Optional.ofNullable(MDC.get(MDC_KEY));
    }

    public static void definir(String valor) {
        MDC.put(MDC_KEY, valor);
    }

    public static void limpar() {
        MDC.remove(MDC_KEY);
    }
}
