package br.com.attus.processos.application.port;

import br.com.attus.processos.domain.model.StatusProcesso;

public record FiltroProcessos(String termo, StatusProcesso status) {

    public static FiltroProcessos vazio() {
        return new FiltroProcessos(null, null);
    }
}
