package br.com.attus.processos.config;

import br.com.attus.processos.application.port.in.AlterarStatusProcessoUseCase;
import br.com.attus.processos.application.port.in.AtualizarProcessoUseCase;
import br.com.attus.processos.application.port.in.BuscarProcessoUseCase;
import br.com.attus.processos.application.port.in.CadastrarProcessoUseCase;
import br.com.attus.processos.application.port.in.ConsultarHistoricoUseCase;
import br.com.attus.processos.application.port.in.ExcluirProcessoUseCase;
import br.com.attus.processos.application.port.in.ListarProcessosUseCase;
import br.com.attus.processos.application.port.in.RegistrarHistoricoUseCase;
import br.com.attus.processos.application.port.out.HistoricoRepositoryPort;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.application.port.out.PublicadorEventosPort;
import br.com.attus.processos.application.usecase.AlterarStatusProcessoService;
import br.com.attus.processos.application.usecase.AtualizarProcessoService;
import br.com.attus.processos.application.usecase.BuscarProcessoService;
import br.com.attus.processos.application.usecase.CadastrarProcessoService;
import br.com.attus.processos.application.usecase.ConsultarHistoricoService;
import br.com.attus.processos.application.usecase.ExcluirProcessoService;
import br.com.attus.processos.application.usecase.ListarProcessosService;
import br.com.attus.processos.application.usecase.ProcessoCarregador;
import br.com.attus.processos.application.usecase.ProcessoPersistencia;
import br.com.attus.processos.application.usecase.RegistrarHistoricoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    ProcessoCarregador processoCarregador(ProcessoRepositoryPort repositorio) {
        return new ProcessoCarregador(repositorio);
    }

    @Bean
    ProcessoPersistencia processoPersistencia(ProcessoRepositoryPort repositorio, PublicadorEventosPort publicador) {
        return new ProcessoPersistencia(repositorio, publicador);
    }

    @Bean
    CadastrarProcessoUseCase cadastrarProcessoUseCase(ProcessoRepositoryPort repositorio,
                                                      ProcessoPersistencia persistencia, Clock clock) {
        return new CadastrarProcessoService(repositorio, persistencia, clock);
    }

    @Bean
    AtualizarProcessoUseCase atualizarProcessoUseCase(ProcessoCarregador carregador, ProcessoPersistencia persistencia, Clock clock) {
        return new AtualizarProcessoService(carregador, persistencia, clock);
    }

    @Bean
    AlterarStatusProcessoUseCase alterarStatusProcessoUseCase(ProcessoCarregador carregador, ProcessoPersistencia persistencia,
                                                              Clock clock) {
        return new AlterarStatusProcessoService(carregador, persistencia, clock);
    }

    @Bean
    ExcluirProcessoUseCase excluirProcessoUseCase(ProcessoCarregador carregador, ProcessoPersistencia persistencia, Clock clock) {
        return new ExcluirProcessoService(carregador, persistencia, clock);
    }

    @Bean
    BuscarProcessoUseCase buscarProcessoUseCase(ProcessoCarregador carregador) {
        return new BuscarProcessoService(carregador);
    }

    @Bean
    ListarProcessosUseCase listarProcessosUseCase(ProcessoRepositoryPort repositorio) {
        return new ListarProcessosService(repositorio);
    }

    @Bean
    RegistrarHistoricoUseCase registrarHistoricoUseCase(HistoricoRepositoryPort repositorio, Clock clock) {
        return new RegistrarHistoricoService(repositorio, clock);
    }

    @Bean
    ConsultarHistoricoUseCase consultarHistoricoUseCase(HistoricoRepositoryPort repositorio) {
        return new ConsultarHistoricoService(repositorio);
    }
}
