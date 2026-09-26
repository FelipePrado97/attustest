package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.CadastrarProcessoUseCase.Comando;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.application.port.out.PublicadorEventosPort;
import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.exception.DadoInvalidoException;
import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.StatusProcesso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static br.com.attus.processos.support.ProcessoFixture.CLOCK;
import static br.com.attus.processos.support.ProcessoFixture.CNJ;
import static br.com.attus.processos.support.ProcessoFixture.dados;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastrarProcessoServiceTest {

    @Mock
    private ProcessoRepositoryPort repositorio;
    @Mock
    private PublicadorEventosPort publicador;
    @Captor
    private ArgumentCaptor<List<ProcessoEvento>> eventos;

    private CadastrarProcessoService service;

    @BeforeEach
    void setUp() {
        service = new CadastrarProcessoService(repositorio, new ProcessoPersistencia(repositorio, publicador), CLOCK);
    }

    @Test
    void cadastraProcessoAtivoEPublicaEventoDeCadastro() {
        when(repositorio.existePorNumero(NumeroCnj.of(CNJ))).thenReturn(false);
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        var processo = service.cadastrar(new Comando(CNJ, dados()));

        assertThat(processo.getStatus()).isEqualTo(StatusProcesso.ATIVO);
        verify(publicador).publicar(eventos.capture());
        assertThat(eventos.getValue()).extracting(ProcessoEvento::tipo).containsExactly(TipoEvento.CADASTRADO);
    }

    @Test
    void rejeitaNumeroDuplicadoSemPersistirNemPublicar() {
        when(repositorio.existePorNumero(NumeroCnj.of(CNJ))).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new Comando(CNJ, dados())))
                .isInstanceOf(ConflitoException.class);

        verify(repositorio, never()).salvar(any());
        verifyNoInteractions(publicador);
    }

    @Test
    void rejeitaNumeroInvalidoAntesDeConsultarORepositorio() {
        assertThatThrownBy(() -> service.cadastrar(new Comando("123", dados())))
                .isInstanceOf(DadoInvalidoException.class);

        verifyNoInteractions(repositorio, publicador);
    }
}
