package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.ExcluirProcessoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class ExcluirProcessoService implements ExcluirProcessoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExcluirProcessoService.class);

    private final ProcessoCarregador carregador;
    private final ProcessoPersistencia persistencia;
    private final Clock clock;

    public ExcluirProcessoService(ProcessoCarregador carregador, ProcessoPersistencia persistencia, Clock clock) {
        this.carregador = carregador;
        this.persistencia = persistencia;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        var processo = carregador.carregar(id);
        processo.marcarExclusao(clock);
        persistencia.excluirEPublicar(processo);
        log.info("Processo excluido [processoId={}, numeroCnj={}]", id, processo.getNumero());
    }
}
