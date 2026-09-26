package br.com.attus.processos.adapter.out.persistence;

import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.support.ProcessoFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessoPersistenceAdapterTraducaoErrosTest {

    @Mock
    private ProcessoJpaRepository repository;

    private ProcessoPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProcessoPersistenceAdapter(repository);
    }

    @Test
    void optimisticLockAoSalvarViraConflitoDeVersao() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(new ObjectOptimisticLockingFailureException(ProcessoJpaEntity.class, "id"));

        assertThatThrownBy(() -> adapter.salvar(ProcessoFixture.persistido()))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("alterado por outro usuário");
    }

    @Test
    void optimisticLockAoExcluirViraConflitoDeVersao() {
        var processo = ProcessoFixture.persistido();
        var entidade = new ProcessoJpaEntity(processo.getId());
        when(repository.findById(processo.getId())).thenReturn(Optional.of(entidade));
        doThrow(new ObjectOptimisticLockingFailureException(ProcessoJpaEntity.class, "id")).when(repository).flush();

        assertThatThrownBy(() -> adapter.excluir(processo.getId())).isInstanceOf(ConflitoException.class);
    }

    @Test
    void violacaoDaConstraintDeNumeroViraConflitoDeDuplicidade() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(violacao(
                "duplicate key value violates unique constraint \"uk_processo_numero_cnj\""));

        assertThatThrownBy(() -> adapter.salvar(ProcessoFixture.persistido()))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Já existe processo");
    }

    @Test
    void outraViolacaoDeIntegridadeNaoEhMascaradaComoConflito() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        var original = violacao("new row violates check constraint \"ck_processo_valor_causa\"");
        when(repository.saveAndFlush(any())).thenThrow(original);

        assertThatThrownBy(() -> adapter.salvar(ProcessoFixture.persistido())).isSameAs(original);
    }

    private static DataIntegrityViolationException violacao(String mensagemDoBanco) {
        return new DataIntegrityViolationException("could not execute statement", new SQLException(mensagemDoBanco));
    }
}
