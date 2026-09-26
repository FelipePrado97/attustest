package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.CadastrarProcessoUseCase;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class CadastrarProcessoService implements CadastrarProcessoUseCase {

    private static final Logger log = LoggerFactory.getLogger(CadastrarProcessoService.class);

    private final ProcessoRepositoryPort repositorio;
    private final ProcessoPersistencia persistencia;
    private final Clock clock;

    public CadastrarProcessoService(ProcessoRepositoryPort repositorio, ProcessoPersistencia persistencia, Clock clock) {
        this.repositorio = repositorio;
        this.persistencia = persistencia;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Processo cadastrar(Comando comando) {
        var numero = NumeroCnj.of(comando.numeroCnj());
        if (repositorio.existePorNumero(numero)) {
            log.warn("Cadastro rejeitado: numero CNJ duplicado [numeroCnj={}]", numero);
            throw ConflitoException.numeroDuplicado(numero);
        }
        var salvo = persistencia.salvarEPublicar(Processo.novo(numero, comando.dados(), clock));
        log.info("Processo cadastrado [processoId={}, numeroCnj={}]", salvo.getId(), numero);
        return salvo;
    }
}
