package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.exception.RecursoNaoEncontradoException;
import br.com.attus.processos.domain.model.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

public class ProcessoCarregador {

    private static final Logger log = LoggerFactory.getLogger(ProcessoCarregador.class);

    private final ProcessoRepositoryPort repositorio;

    public ProcessoCarregador(ProcessoRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    public Processo carregar(UUID id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> RecursoNaoEncontradoException.processo(id));
    }

    public Processo carregarNaVersao(UUID id, Long versaoEsperada) {
        var processo = carregar(id);
        if (!Objects.equals(processo.getVersao(), versaoEsperada)) {
            log.warn("Conflito de versao [processoId={}, esperada={}, atual={}]", id, versaoEsperada, processo.getVersao());
            throw ConflitoException.versaoDesatualizada();
        }
        return processo;
    }
}
