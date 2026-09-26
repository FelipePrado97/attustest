package br.com.attus.processos.domain.model;

import br.com.attus.processos.domain.event.ProcessoEvento;
import br.com.attus.processos.domain.event.TipoEvento;
import br.com.attus.processos.domain.exception.DadoInvalidoException;
import br.com.attus.processos.domain.exception.ProcessoArquivadoException;
import br.com.attus.processos.domain.exception.TransicaoStatusInvalidaException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Processo {

    public static final int TAMANHO_MAXIMO_ASSUNTO = 200;
    public static final int TAMANHO_MAXIMO_PARTE = 150;

    public static final int DIGITOS_INTEIROS_VALOR = 13;
    public static final int CASAS_DECIMAIS_VALOR = 2;

    public static final LocalDate DATA_MINIMA_DISTRIBUICAO = LocalDate.of(1900, 1, 1);

    private final UUID id;
    private final NumeroCnj numero;
    private String assunto;
    private String parteContraria;
    private BigDecimal valorCausa;
    private LocalDate dataDistribuicao;
    private StatusProcesso status;
    private final Long versao;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    private final List<ProcessoEvento> eventos = new ArrayList<>();

    private Processo(UUID id, NumeroCnj numero, DadosValidados dados, StatusProcesso status,
                     Long versao, Instant criadoEm, Instant atualizadoEm) {
        this.id = Objects.requireNonNull(id, "id");
        this.numero = Objects.requireNonNull(numero, "numero");
        this.status = Objects.requireNonNull(status, "status");
        this.versao = versao;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        aplicar(dados);
    }

    public static Processo novo(NumeroCnj numero, DadosProcesso dados, Clock clock) {
        Objects.requireNonNull(numero, "numero");
        var validados = DadosValidados.paraGravacao(dados, numero, clock);
        var agora = Instant.now(clock);
        var processo = new Processo(UUID.randomUUID(), numero, validados, StatusProcesso.ATIVO, null, agora, agora);
        processo.registrar(TipoEvento.CADASTRADO, "Processo cadastrado", clock);
        return processo;
    }

    public static Processo reconstituir(UUID id, NumeroCnj numero, DadosProcesso dados, StatusProcesso status,
                                        Long versao, Instant criadoEm, Instant atualizadoEm) {
        return new Processo(id, numero, DadosValidados.jaGravados(dados), status, versao, criadoEm, atualizadoEm);
    }

    public void atualizar(DadosProcesso dados, Clock clock) {
        if (!status.permiteEdicao()) {
            throw new ProcessoArquivadoException(numero);
        }
        aplicar(DadosValidados.paraGravacao(dados, numero, clock));
        atualizadoEm = Instant.now(clock);
        registrar(TipoEvento.ATUALIZADO, "Dados do processo atualizados", clock);
    }

    public void alterarStatus(StatusProcesso novoStatus, Clock clock) {
        if (novoStatus == null) {
            throw new DadoInvalidoException("status", "Novo status é obrigatório");
        }
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(status, novoStatus);
        }
        var anterior = status;
        status = novoStatus;
        atualizadoEm = Instant.now(clock);
        registrar(TipoEvento.STATUS_ALTERADO, "Status alterado de %s para %s".formatted(anterior, novoStatus), clock);
    }

    public void marcarExclusao(Clock clock) {
        registrar(TipoEvento.EXCLUIDO, "Processo excluído", clock);
    }

    public List<ProcessoEvento> extrairEventos() {
        var copia = List.copyOf(eventos);
        eventos.clear();
        return copia;
    }

    private void aplicar(DadosValidados dados) {
        this.assunto = dados.assunto();
        this.parteContraria = dados.parteContraria();
        this.valorCausa = dados.valorCausa();
        this.dataDistribuicao = dados.dataDistribuicao();
    }

    private void registrar(TipoEvento tipo, String descricao, Clock clock) {
        eventos.add(new ProcessoEvento(UUID.randomUUID(), tipo, id, numero.valor(), descricao, Instant.now(clock)));
    }

    private record DadosValidados(String assunto, String parteContraria, BigDecimal valorCausa, LocalDate dataDistribuicao) {

        private static final String CAMPO_DATA = "dataDistribuicao";
        private static final String CAMPO_VALOR = "valorCausa";
        private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        static DadosValidados paraGravacao(DadosProcesso dados, NumeroCnj numero, Clock clock) {
            var validados = jaGravados(dados);
            validarIntervaloDaDistribuicao(validados.dataDistribuicao(), numero, clock);
            return validados;
        }

        static DadosValidados jaGravados(DadosProcesso dados) {
            if (dados == null) {
                throw new DadoInvalidoException("dados", "Dados do processo são obrigatórios");
            }
            if (dados.dataDistribuicao() == null) {
                throw new DadoInvalidoException(CAMPO_DATA, "Campo obrigatório");
            }
            return new DadosValidados(
                    textoObrigatorio("assunto", dados.assunto(), TAMANHO_MAXIMO_ASSUNTO),
                    textoObrigatorio("parteContraria", dados.parteContraria(), TAMANHO_MAXIMO_PARTE),
                    valorMonetario(dados.valorCausa()),
                    dados.dataDistribuicao());
        }

        private static String textoObrigatorio(String campo, String valor, int tamanhoMaximo) {
            if (valor == null || valor.isBlank()) {
                throw new DadoInvalidoException(campo, "Campo obrigatório");
            }
            var normalizado = valor.strip();
            if (normalizado.length() > tamanhoMaximo) {
                throw new DadoInvalidoException(campo, "Deve ter no máximo " + tamanhoMaximo + " caracteres");
            }
            return normalizado;
        }

        private static BigDecimal valorMonetario(BigDecimal valor) {
            if (valor == null) {
                throw new DadoInvalidoException(CAMPO_VALOR, "Campo obrigatório");
            }
            if (valor.signum() < 0) {
                throw new DadoInvalidoException(CAMPO_VALOR, "Valor da causa deve ser maior ou igual a zero");
            }
            var semZerosFinais = valor.stripTrailingZeros();
            if (semZerosFinais.scale() > CASAS_DECIMAIS_VALOR) {
                throw new DadoInvalidoException(CAMPO_VALOR, "Valor da causa deve ter no máximo " + CASAS_DECIMAIS_VALOR + " casas decimais");
            }
            if (semZerosFinais.precision() - semZerosFinais.scale() > DIGITOS_INTEIROS_VALOR) {
                throw new DadoInvalidoException(CAMPO_VALOR, "Valor da causa excede o limite permitido");
            }
            return valor.setScale(CASAS_DECIMAIS_VALOR);
        }

        private static void validarIntervaloDaDistribuicao(LocalDate data, NumeroCnj numero, Clock clock) {
            exigirAPartirDaDataMinima(data);
            exigirAPartirDoAnoDeAjuizamento(data, numero.anoAjuizamento());
            exigirQueNaoSejaFutura(data, clock);
        }

        private static void exigirAPartirDaDataMinima(LocalDate data) {
            if (data.isBefore(DATA_MINIMA_DISTRIBUICAO)) {
                throw new DadoInvalidoException(CAMPO_DATA,
                        "Data de distribuição não pode ser anterior a " + DATA_MINIMA_DISTRIBUICAO.format(DATA_BR));
            }
        }

        private static void exigirAPartirDoAnoDeAjuizamento(LocalDate data, int anoAjuizamento) {
            if (data.getYear() < anoAjuizamento) {
                throw new DadoInvalidoException(CAMPO_DATA,
                        "Data de distribuição não pode ser anterior ao ano de ajuizamento do processo (" + anoAjuizamento + ")");
            }
        }

        private static void exigirQueNaoSejaFutura(LocalDate data, Clock clock) {
            if (data.isAfter(LocalDate.now(clock))) {
                throw new DadoInvalidoException(CAMPO_DATA, "Data de distribuição não pode ser futura");
            }
        }
    }

    public UUID getId() { return id; }
    public NumeroCnj getNumero() { return numero; }
    public String getAssunto() { return assunto; }
    public String getParteContraria() { return parteContraria; }
    public BigDecimal getValorCausa() { return valorCausa; }
    public LocalDate getDataDistribuicao() { return dataDistribuicao; }
    public StatusProcesso getStatus() { return status; }
    public Long getVersao() { return versao; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
