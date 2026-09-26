package br.com.attus.processos.adapter.out.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
class OutboxEventoEntity {

    static final int TAMANHO_MAXIMO_ERRO = 1000;

    @Id
    private UUID id;

    @Column(insertable = false, updatable = false)
    private Long sequencia;

    @Column(name = "agregado_id", nullable = false)
    private UUID agregadoId;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;

    @Column(name = "ultimo_erro", length = TAMANHO_MAXIMO_ERRO)
    private String ultimoErro;

    protected OutboxEventoEntity() {
    }

    OutboxEventoEntity(UUID id, UUID agregadoId, String tipo, String payload, String correlationId, Instant criadoEm) {
        this.id = id;
        this.agregadoId = agregadoId;
        this.tipo = tipo;
        this.payload = payload;
        this.correlationId = correlationId;
        this.criadoEm = criadoEm;
    }

    boolean aguardandoBackoff(Instant agora) {
        return proximaTentativaEm != null && proximaTentativaEm.isAfter(agora);
    }

    void marcarPublicado(Instant quando) {
        this.publicadoEm = quando;
        this.proximaTentativaEm = null;
        this.ultimoErro = null;
    }

    void registrarFalha(String erro, Instant proximaTentativa) {
        this.tentativas++;
        this.proximaTentativaEm = proximaTentativa;
        this.ultimoErro = erro.length() > TAMANHO_MAXIMO_ERRO ? erro.substring(0, TAMANHO_MAXIMO_ERRO) : erro;
    }

    UUID getId() { return id; }
    UUID getAgregadoId() { return agregadoId; }
    String getTipo() { return tipo; }
    String getPayload() { return payload; }
    String getCorrelationId() { return correlationId; }
    int getTentativas() { return tentativas; }
    Instant getPublicadoEm() { return publicadoEm; }
    Instant getProximaTentativaEm() { return proximaTentativaEm; }
    String getUltimoErro() { return ultimoErro; }
}
