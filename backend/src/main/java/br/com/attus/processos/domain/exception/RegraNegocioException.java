package br.com.attus.processos.domain.exception;

public sealed class RegraNegocioException extends DominioException
        permits TransicaoStatusInvalidaException, ProcessoArquivadoException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
