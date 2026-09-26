package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.in.BuscarProcessoUseCase;
import br.com.attus.processos.domain.model.Processo;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class BuscarProcessoService implements BuscarProcessoUseCase {

    private final ProcessoCarregador carregador;

    public BuscarProcessoService(ProcessoCarregador carregador) {
        this.carregador = carregador;
    }

    @Override
    @Transactional(readOnly = true)
    public Processo buscarPorId(UUID id) {
        return carregador.carregar(id);
    }
}
