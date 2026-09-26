package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscarProcessoServiceTest {

    @Mock
    private ProcessoRepositoryPort repositorio;

    @Test
    void retornaProcessoExistente() {
        var existente = persistido();
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        assertThat(new BuscarProcessoService(new ProcessoCarregador(repositorio)).buscarPorId(existente.getId()))
                .isSameAs(existente);
    }

    @Test
    void processoInexistenteLancaNaoEncontradoComOId() {
        var id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new BuscarProcessoService(new ProcessoCarregador(repositorio)).buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }
}
