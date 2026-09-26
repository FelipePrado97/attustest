package br.com.attus.processos.domain.model;

import br.com.attus.processos.domain.event.TipoEvento;

import java.time.Instant;
import java.util.UUID;

public record RegistroHistorico(
        UUID eventoId,
        UUID processoId,
        TipoEvento tipo,
        String descricao,
        Instant ocorridoEm,
        Instant registradoEm) {
}
