package br.com.attus.processos.adapter.out.persistence;

import br.com.attus.processos.domain.model.NumeroCnj;
import br.com.attus.processos.domain.model.StatusProcesso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "processo")
class ProcessoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "numero_cnj", nullable = false, unique = true, length = 25)
    private String numeroCnj;

    @Column(name = "numero_cnj_digitos", nullable = false, updatable = false, length = 20)
    private String numeroCnjDigitos;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Column(name = "parte_contraria", nullable = false, length = 150)
    private String parteContraria;

    @Column(name = "valor_causa", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCausa;

    @Column(name = "data_distribuicao", nullable = false)
    private LocalDate dataDistribuicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProcesso status;

    @Version
    private Long versao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ProcessoJpaEntity() {
    }

    ProcessoJpaEntity(UUID id) {
        this.id = id;
    }

    UUID getId() { return id; }
    String getNumeroCnj() { return numeroCnj; }

    void definirNumeroCnj(NumeroCnj numero) {
        this.numeroCnj = numero.valor();
        this.numeroCnjDigitos = NumeroCnj.somenteDigitos(numero.valor());
    }
    String getAssunto() { return assunto; }
    void setAssunto(String assunto) { this.assunto = assunto; }
    String getParteContraria() { return parteContraria; }
    void setParteContraria(String parteContraria) { this.parteContraria = parteContraria; }
    BigDecimal getValorCausa() { return valorCausa; }
    void setValorCausa(BigDecimal valorCausa) { this.valorCausa = valorCausa; }
    LocalDate getDataDistribuicao() { return dataDistribuicao; }
    void setDataDistribuicao(LocalDate dataDistribuicao) { this.dataDistribuicao = dataDistribuicao; }
    StatusProcesso getStatus() { return status; }
    void setStatus(StatusProcesso status) { this.status = status; }
    Long getVersao() { return versao; }
    Instant getCriadoEm() { return criadoEm; }
    void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    Instant getAtualizadoEm() { return atualizadoEm; }
    void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
