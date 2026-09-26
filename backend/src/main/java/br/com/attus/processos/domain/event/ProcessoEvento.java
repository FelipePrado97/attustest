package br.com.attus.processos.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ProcessoEvento(
        UUID eventoId,
        TipoEvento tipo,
        UUID processoId,
        String numeroCnj,
        String descricao,
        Instant ocorridoEm) {
}
