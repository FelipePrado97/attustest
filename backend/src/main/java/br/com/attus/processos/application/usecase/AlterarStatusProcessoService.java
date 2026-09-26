package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.AlterarStatusProcessoUseCase;
import br.com.attus.processos.domain.model.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class AlterarStatusProcessoService implements AlterarStatusProcessoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AlterarStatusProcessoService.class);

    private final ProcessoCarregador carregador;
    private final ProcessoPersistencia persistencia;
    private final Clock clock;

    public AlterarStatusProcessoService(ProcessoCarregador carregador, ProcessoPersistencia persistencia, Clock clock) {
        this.carregador = carregador;
        this.persistencia = persistencia;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Processo alterarStatus(Comando comando) {
        var processo = carregador.carregarNaVersao(comando.id(), comando.versao());
        var anterior = processo.getStatus();
        processo.alterarStatus(comando.novoStatus(), clock);
        var salvo = persistencia.salvarEPublicar(processo);
        log.info("Status do processo alterado [processoId={}, de={}, para={}]", salvo.getId(), anterior, salvo.getStatus());
        return salvo;
    }
}
