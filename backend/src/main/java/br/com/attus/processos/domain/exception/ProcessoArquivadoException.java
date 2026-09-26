package br.com.attus.processos.domain.exception;

import br.com.attus.processos.domain.model.NumeroCnj;

public final class ProcessoArquivadoException extends RegraNegocioException {

    public ProcessoArquivadoException(NumeroCnj numero) {
        super("Processo " + numero + " está arquivado e não pode ser editado. Desarquive-o antes");
    }
}
