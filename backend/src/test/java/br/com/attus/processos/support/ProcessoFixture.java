package br.com.attus.processos.support;

import br.com.attus.processos.domain.model.DadosProcesso;
import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.Processo;
import br.com.attus.processos.domain.model.StatusProcesso;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public final class ProcessoFixture {

    public static final String CNJ = "0001234-71.2024.8.26.0100";
    public static final Instant AGORA = Instant.parse("2025-06-10T15:00:00Z");
    public static final Clock CLOCK = Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo"));

    private ProcessoFixture() {
    }

    public static DadosProcesso dados() {
        return new DadosProcesso("Execução fiscal", "Contribuinte", new BigDecimal("1500.00"), LocalDate.of(2024, 3, 15));
    }

    public static Processo persistido(StatusProcesso status, Long versao) {
        return Processo.reconstituir(UUID.randomUUID(), NumeroCnj.of(CNJ), dados(), status, versao, AGORA, AGORA);
    }

    public static Processo persistido() {
        return persistido(StatusProcesso.ATIVO, 0L);
    }
}
