package br.com.attus.processos.adapter.in.web.processo.dto;

import br.com.attus.processos.application.port.Pagina;

import java.util.List;
import java.util.function.Function;

public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {

    public static <D, T> PaginaResponse<T> de(Pagina<D> pagina, Function<D, T> mapper) {
        return new PaginaResponse<>(pagina.itens().stream().map(mapper).toList(), pagina.pagina(),
                pagina.tamanho(), pagina.totalItens(), pagina.totalPaginas());
    }
}
