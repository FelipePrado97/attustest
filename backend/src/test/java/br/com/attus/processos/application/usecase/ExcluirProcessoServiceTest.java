package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.application.port.out.PublicadorEventosPort;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.CLOCK;
import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcluirProcessoServiceTest {

    @Mock
    private ProcessoRepositoryPort repositorio;
    @Mock
    private PublicadorEventosPort publicador;
    @Captor
    private ArgumentCaptor<List<ProcessoEvento>> eventos;

    private ExcluirProcessoService service;

    @BeforeEach
    void setUp() {
        service = new ExcluirProcessoService(new ProcessoCarregador(repositorio),
                new ProcessoPersistencia(repositorio, publicador), CLOCK);
    }

    @Test
    void excluiEPublicaEventoDeExclusao() {
        var existente = persistido();
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        service.excluir(existente.getId());

        verify(repositorio).excluir(existente.getId());
        verify(publicador).publicar(eventos.capture());
        assertThat(eventos.getValue()).extracting(ProcessoEvento::tipo).containsExactly(TipoEvento.EXCLUIDO);
    }

    @Test
    void processoInexistenteNaoExcluiNemPublica() {
        var id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id)).isInstanceOf(RecursoNaoEncontradoException.class);

        verify(repositorio, never()).excluir(any());
        verifyNoInteractions(publicador);
    }
}
