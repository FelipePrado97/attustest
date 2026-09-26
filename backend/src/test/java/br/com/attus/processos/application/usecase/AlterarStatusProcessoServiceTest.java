package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.AlterarStatusProcessoUseCase.Comando;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.application.port.out.PublicadorEventosPort;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.exception.TransicaoStatusInvalidaException;
import br.com.attus.processos.domain.model.StatusProcesso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
class AlterarStatusProcessoServiceTest {

    @Mock
    private ProcessoRepositoryPort repositorio;
    @Mock
    private PublicadorEventosPort publicador;
    @Captor
    private ArgumentCaptor<List<ProcessoEvento>> eventos;

    private AlterarStatusProcessoService service;

    @BeforeEach
    void setUp() {
        service = new AlterarStatusProcessoService(new ProcessoCarregador(repositorio),
                new ProcessoPersistencia(repositorio, publicador), CLOCK);
    }

    @Test
    void alteraStatusEPublicaEvento() {
        var existente = persistido(StatusProcesso.ATIVO, 0L);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        var alterado = service.alterarStatus(new Comando(existente.getId(), 0L, StatusProcesso.SUSPENSO));

        assertThat(alterado.getStatus()).isEqualTo(StatusProcesso.SUSPENSO);
        verify(publicador).publicar(eventos.capture());
        assertThat(eventos.getValue()).extracting(ProcessoEvento::tipo).containsExactly(TipoEvento.STATUS_ALTERADO);
    }

    @Test
    void transicaoInvalidaNaoPersiste() {
        var existente = persistido(StatusProcesso.ARQUIVADO, 0L);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.alterarStatus(new Comando(existente.getId(), 0L, StatusProcesso.SUSPENSO)))
                .isInstanceOf(TransicaoStatusInvalidaException.class);

        verify(repositorio, never()).salvar(any());
        verifyNoInteractions(publicador);
    }

    @Test
    void versaoDesatualizadaRetornaConflito() {
        var existente = persistido(StatusProcesso.ATIVO, 5L);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.alterarStatus(new Comando(existente.getId(), 4L, StatusProcesso.SUSPENSO)))
                .isInstanceOf(ConflitoException.class);
    }
}
