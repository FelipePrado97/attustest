package br.com.attus.processos.application.port.in;

import br.com.attus.processos.domain.model.DadosProcesso;
import br.com.attus.processos.domain.model.Processo;

import java.util.UUID;

public interface AtualizarProcessoUseCase {

    Processo atualizar(Comando comando);

    record Comando(UUID id, Long versao, DadosProcesso dados) {
    }
}
