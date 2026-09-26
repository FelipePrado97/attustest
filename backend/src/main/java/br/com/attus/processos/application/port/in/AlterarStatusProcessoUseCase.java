package br.com.attus.processos.application.port.in;

import br.com.attus.processos.domain.model.Processo;
import br.com.attus.processos.domain.model.StatusProcesso;

import java.util.UUID;

public interface AlterarStatusProcessoUseCase {

    Processo alterarStatus(Comando comando);

    record Comando(UUID id, Long versao, StatusProcesso novoStatus) {
    }
}
