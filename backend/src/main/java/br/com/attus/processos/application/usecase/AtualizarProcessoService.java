package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.AtualizarProcessoUseCase;
import br.com.attus.processos.domain.model.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class AtualizarProcessoService implements AtualizarProcessoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AtualizarProcessoService.class);

    private final ProcessoCarregador carregador;
    private final ProcessoPersistencia persistencia;
    private final Clock clock;

    public AtualizarProcessoService(ProcessoCarregador carregador, ProcessoPersistencia persistencia, Clock clock) {
        this.carregador = carregador;
        this.persistencia = persistencia;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Processo atualizar(Comando comando) {
        var processo = carregador.carregarNaVersao(comando.id(), comando.versao());
        processo.atualizar(comando.dados(), clock);
        var salvo = persistencia.salvarEPublicar(processo);
        log.info("Processo atualizado [processoId={}, versao={}]", salvo.getId(), salvo.getVersao());
        return salvo;
    }
}
