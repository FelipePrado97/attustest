package br.com.attus.processos.domain.exception;

import java.util.UUID;

public final class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException processo(UUID id) {
        return new RecursoNaoEncontradoException("Processo não encontrado: " + id);
    }
}
