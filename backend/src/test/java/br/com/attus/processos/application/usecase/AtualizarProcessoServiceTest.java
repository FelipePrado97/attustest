package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.AtualizarProcessoUseCase.Comando;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.application.port.out.PublicadorEventosPort;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.exception.ProcessoArquivadoException;
import br.com.attus.processos.domain.exception.RecursoNaoEncontradoException;
import br.com.attus.processos.domain.model.DadosProcesso;
import br.com.attus.processos.domain.model.StatusProcesso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.CLOCK;
import static br.com.attus.processos.support.ProcessoFixture.dados;
import static br.com.attus.processos.support.ProcessoFixture.persistido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtualizarProcessoServiceTest {

    @Mock
    private ProcessoRepositoryPort repositorio;
    @Mock
    private PublicadorEventosPort publicador;
    @Captor
    private ArgumentCaptor<List<ProcessoEvento>> eventos;

    private AtualizarProcessoService service;

    @BeforeEach
    void setUp() {
        service = new AtualizarProcessoService(new ProcessoCarregador(repositorio),
                new ProcessoPersistencia(repositorio, publicador), CLOCK);
    }

    @Test
    void atualizaNaVersaoCorretaEPublicaEvento() {
        var existente = persistido(StatusProcesso.ATIVO, 3L);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        var novosDados = new DadosProcesso("Novo assunto", "Nova parte", new BigDecimal("50.00"), LocalDate.of(2024, 5, 5));
        var atualizado = service.atualizar(new Comando(existente.getId(), 3L, novosDados));

        assertThat(atualizado.getAssunto()).isEqualTo("Novo assunto");
        verify(publicador).publicar(eventos.capture());
        assertThat(eventos.getValue()).extracting(ProcessoEvento::tipo).containsExactly(TipoEvento.ATUALIZADO);
    }

    @Test
    void versaoDesatualizadaRetornaConflitoSemPersistir() {
        var existente = persistido(StatusProcesso.ATIVO, 3L);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(new Comando(existente.getId(), 2L, dados())))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("alterado por outro usuário");

        verify(repositorio, never()).salvar(any());
        verifyNoInteractions(publicador);
    }

    @Test
    void processoArquivadoNaoPodeSerEditado() {
        var existente = persistido(StatusProcesso.ARQUIVADO, 0L);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(new Comando(existente.getId(), 0L, dados())))
                .isInstanceOf(ProcessoArquivadoException.class);

        verify(repositorio, never()).salvar(any());
    }

    @Test
    void processoInexistenteLancaNaoEncontrado() {
        var id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(new Comando(id, 0L, dados())))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
