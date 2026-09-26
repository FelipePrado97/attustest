package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.application.port.Pagina;
import br.com.attus.processos.application.port.in.ListarProcessosUseCase;
import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.domain.model.Processo;
import org.springframework.transaction.annotation.Transactional;

public class ListarProcessosService implements ListarProcessosUseCase {

    private final ProcessoRepositoryPort repositorio;

    public ListarProcessosService(ProcessoRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Processo> listar(FiltroProcessos filtro, int pagina, int tamanho) {
        return repositorio.buscar(filtro == null ? FiltroProcessos.vazio() : filtro, pagina, tamanho);
    }
}
