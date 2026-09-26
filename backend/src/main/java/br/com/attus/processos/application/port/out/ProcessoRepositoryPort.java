package br.com.attus.processos.application.port.out;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.application.port.Pagina;
import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.Processo;

import java.util.Optional;
import java.util.UUID;

public interface ProcessoRepositoryPort {

    Processo salvar(Processo processo);

    Optional<Processo> buscarPorId(UUID id);

    boolean existePorNumero(NumeroCnj numero);

    Pagina<Processo> buscar(FiltroProcessos filtro, int pagina, int tamanho);

    void excluir(UUID id);
}
