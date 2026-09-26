package br.com.attus.processos.application.port.in;

import br.com.attus.processos.domain.model.DadosProcesso;
import br.com.attus.processos.domain.model.Processo;

public interface CadastrarProcessoUseCase {

    Processo cadastrar(Comando comando);

    record Comando(String numeroCnj, DadosProcesso dados) {
    }
}
