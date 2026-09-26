package br.com.attus.processos.adapter.out.persistence;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.application.port.Pagina;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.model.DadosProcesso;
import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.Processo;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
class ProcessoPersistenceAdapter implements ProcessoRepositoryPort {

    static final String CONSTRAINT_NUMERO_CNJ = "uk_processo_numero_cnj";

    private static final Sort MAIS_RECENTES_PRIMEIRO_COM_DESEMPATE_POR_ID = Sort.by(Sort.Order.desc("atualizadoEm"), Sort.Order.asc("id"));

    private final ProcessoJpaRepository repository;

    ProcessoPersistenceAdapter(ProcessoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Processo salvar(Processo processo) {
        var entidade = repository.findById(processo.getId()).orElseGet(() -> novaEntidade(processo));
        copiarDadosEditaveis(processo, entidade);
        try {
            return paraDominio(repository.saveAndFlush(entidade));
        } catch (OptimisticLockingFailureException e) {
            throw ConflitoException.versaoDesatualizada();
        } catch (DataIntegrityViolationException e) {
            if (violouConstraint(e, CONSTRAINT_NUMERO_CNJ)) {
                throw ConflitoException.numeroDuplicado(processo.getNumero());
            }
            throw e;
        }
    }

    @Override
    public Optional<Processo> buscarPorId(UUID id) {
        return repository.findById(id).map(ProcessoPersistenceAdapter::paraDominio);
    }

    @Override
    public boolean existePorNumero(NumeroCnj numero) {
        return repository.existsByNumeroCnj(numero.valor());
    }

    @Override
    public Pagina<Processo> buscar(FiltroProcessos filtro, int pagina, int tamanho) {
        var page = repository.findAll(ProcessoSpecifications.de(filtro), PageRequest.of(pagina, tamanho, MAIS_RECENTES_PRIMEIRO_COM_DESEMPATE_POR_ID));
        return new Pagina<>(page.getContent().stream().map(ProcessoPersistenceAdapter::paraDominio).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public void excluir(UUID id) {
        repository.findById(id).ifPresent(entidade -> {
            try {
                repository.delete(entidade);
                repository.flush();
            } catch (OptimisticLockingFailureException e) {
                throw ConflitoException.versaoDesatualizada();
            }
        });
    }

    private static boolean violouConstraint(DataIntegrityViolationException e, String constraint) {
        var mensagem = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
        return mensagem != null && mensagem.toLowerCase(Locale.ROOT).contains(constraint);
    }

    private static void copiarDadosEditaveis(Processo processo, ProcessoJpaEntity entidade) {
        entidade.setAssunto(processo.getAssunto());
        entidade.setParteContraria(processo.getParteContraria());
        entidade.setValorCausa(processo.getValorCausa());
        entidade.setDataDistribuicao(processo.getDataDistribuicao());
        entidade.setStatus(processo.getStatus());
        entidade.setAtualizadoEm(processo.getAtualizadoEm());
    }

    private static ProcessoJpaEntity novaEntidade(Processo processo) {
        var entidade = new ProcessoJpaEntity(processo.getId());
        entidade.definirNumeroCnj(processo.getNumero());
        entidade.setCriadoEm(processo.getCriadoEm());
        return entidade;
    }

    private static Processo paraDominio(ProcessoJpaEntity e) {
        return Processo.reconstituir(
                e.getId(),
                new NumeroCnj(e.getNumeroCnj()),
                new DadosProcesso(e.getAssunto(), e.getParteContraria(), e.getValorCausa(), e.getDataDistribuicao()),
                e.getStatus(),
                e.getVersao(),
                e.getCriadoEm(),
                e.getAtualizadoEm());
    }
}
