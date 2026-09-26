package br.com.attus.processos.domain.exception;

public final class DadoInvalidoException extends DominioException {

    private final String campo;

    public DadoInvalidoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
