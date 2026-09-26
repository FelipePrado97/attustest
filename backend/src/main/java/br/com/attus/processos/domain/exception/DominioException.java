package br.com.attus.processos.domain.exception;

public abstract sealed class DominioException extends RuntimeException
        permits DadoInvalidoException, RecursoNaoEncontradoException, ConflitoException, RegraNegocioException {

    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
