package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.out.HistoricoRepositoryPort;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.model.RegistroHistorico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static br.com.attus.processos.support.ProcessoFixture.AGORA;
import static br.com.attus.processos.support.ProcessoFixture.CLOCK;
import static br.com.attus.processos.support.ProcessoFixture.CNJ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarHistoricoServiceTest {

    @Mock
    private HistoricoRepositoryPort repositorio;

    private RegistrarHistoricoService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarHistoricoService(repositorio, CLOCK);
    }

    @Test
    void registraEventoNovo() {
        var evento = evento();
        when(repositorio.existeEvento(evento.eventoId())).thenReturn(false);

        assertThat(service.registrar(evento)).isTrue();

        var captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(repositorio).salvar(captor.capture());
        assertThat(captor.getValue().eventoId()).isEqualTo(evento.eventoId());
        assertThat(captor.getValue().registradoEm()).isEqualTo(AGORA);
    }

    @Test
    void ignoraEventoJaProcessado_idempotencia() {
        var evento = evento();
        when(repositorio.existeEvento(evento.eventoId())).thenReturn(true);

        assertThat(service.registrar(evento)).isFalse();

        verify(repositorio, never()).salvar(any());
    }

    private static ProcessoEvento evento() {
        return new ProcessoEvento(UUID.randomUUID(), TipoEvento.CADASTRADO, UUID.randomUUID(), CNJ, "Processo cadastrado", AGORA);
    }
}
