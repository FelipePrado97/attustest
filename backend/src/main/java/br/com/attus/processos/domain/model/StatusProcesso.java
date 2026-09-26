package br.com.attus.processos.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum StatusProcesso {

    ATIVO,
    SUSPENSO,
    ARQUIVADO;

    public Set<StatusProcesso> transicoesPermitidas() {
        return switch (this) {
            case ATIVO -> EnumSet.of(SUSPENSO, ARQUIVADO);
            case SUSPENSO -> EnumSet.of(ATIVO, ARQUIVADO);
            case ARQUIVADO -> EnumSet.of(ATIVO);
        };
    }

    public boolean podeTransicionarPara(StatusProcesso destino) {
        return transicoesPermitidas().contains(destino);
    }

    public boolean permiteEdicao() {
        return this != ARQUIVADO;
    }
}
