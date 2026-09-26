package br.com.attus.processos.adapter.in.web.processo.dto;

import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.model.RegistroHistorico;

import java.time.Instant;
import java.util.UUID;

public record HistoricoResponse(UUID eventoId, TipoEvento tipo, String descricao, Instant ocorridoEm) {

    public static HistoricoResponse de(RegistroHistorico r) {
        return new HistoricoResponse(r.eventoId(), r.tipo(), r.descricao(), r.ocorridoEm());
    }
}
