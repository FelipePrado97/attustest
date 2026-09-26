package br.com.attus.processos.application.port.in;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.application.port.Pagina;
import br.com.attus.processos.domain.model.Processo;

public interface ListarProcessosUseCase {

    Pagina<Processo> listar(FiltroProcessos filtro, int pagina, int tamanho);
}
