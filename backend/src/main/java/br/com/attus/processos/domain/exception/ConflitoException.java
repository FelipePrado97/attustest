package br.com.attus.processos.domain.exception;

import br.com.attus.processos.domain.model.NumeroCnj;

public final class ConflitoException extends DominioException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }

    public static ConflitoException numeroDuplicado(NumeroCnj numero) {
        return new ConflitoException("Já existe processo cadastrado com o número " + numero);
    }

    public static ConflitoException versaoDesatualizada() {
        return new ConflitoException("O processo foi alterado por outro usuário. Recarregue os dados e tente novamente");
    }
}
