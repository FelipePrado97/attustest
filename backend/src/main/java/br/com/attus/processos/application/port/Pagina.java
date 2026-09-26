package br.com.attus.processos.application.port;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalItens) {

    public int totalPaginas() {
        return tamanho == 0 ? 0 : (int) Math.ceil((double) totalItens / tamanho);
    }

    public <R> Pagina<R> map(Function<T, R> mapper) {
        return new Pagina<>(itens.stream().map(mapper).toList(), pagina, tamanho, totalItens);
    }
}
