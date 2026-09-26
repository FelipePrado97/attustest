package br.com.attus.processos.domain.exception;

import br.com.attus.processos.domain.model.StatusProcesso;

public final class TransicaoStatusInvalidaException extends RegraNegocioException {

    public TransicaoStatusInvalidaException(StatusProcesso origem, StatusProcesso destino) {
        super("Transição de status não permitida: %s -> %s. Permitidas a partir de %s: %s"
                .formatted(origem, destino, origem, origem.transicoesPermitidas()));
    }
}
