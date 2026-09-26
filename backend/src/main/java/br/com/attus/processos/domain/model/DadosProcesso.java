package br.com.attus.processos.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosProcesso(
        String assunto,
        String parteContraria,
        BigDecimal valorCausa,
        LocalDate dataDistribuicao) {
}
