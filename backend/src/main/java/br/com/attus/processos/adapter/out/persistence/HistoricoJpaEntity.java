package br.com.attus.processos.adapter.out.persistence;

import br.com.attus.processos.domain.event.TipoEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "historico_processo")
class HistoricoJpaEntity {

    @Id
    @Column(name = "evento_id")
    private UUID eventoId;

    @Column(insertable = false, updatable = false)
    private Long sequencia;

    @Column(name = "processo_id", nullable = false)
    private UUID processoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEvento tipo;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    protected HistoricoJpaEntity() {
    }

    HistoricoJpaEntity(UUID eventoId, UUID processoId, TipoEvento tipo, String descricao,
                       Instant ocorridoEm, Instant registradoEm) {
        this.eventoId = eventoId;
        this.processoId = processoId;
        this.tipo = tipo;
        this.descricao = descricao;
        this.ocorridoEm = ocorridoEm;
        this.registradoEm = registradoEm;
    }

    UUID getEventoId() { return eventoId; }
    UUID getProcessoId() { return processoId; }
    TipoEvento getTipo() { return tipo; }
    String getDescricao() { return descricao; }
    Instant getOcorridoEm() { return ocorridoEm; }
    Instant getRegistradoEm() { return registradoEm; }
}
