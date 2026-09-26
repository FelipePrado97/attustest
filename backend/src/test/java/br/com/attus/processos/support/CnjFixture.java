package br.com.attus.processos.support;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

public final class CnjFixture {

    private static final int ANO_PADRAO = 2024;
    private static final AtomicInteger SEQUENCIAL = new AtomicInteger(1);

    private CnjFixture() {
    }

    public static String proximo() {
        return gerar(SEQUENCIAL.getAndIncrement(), ANO_PADRAO);
    }

    public static String comAno(int ano) {
        return gerar(SEQUENCIAL.getAndIncrement(), ano);
    }

    private static String gerar(int sequencial, int anoAjuizamento) {
        var n = "%07d".formatted(sequencial);
        var ano = "%04d".formatted(anoAjuizamento);
        var justica = "8";
        var tribunal = "26";
        var origem = "0100";
        var dv = 98 - new BigInteger(n + ano + justica + tribunal + origem + "00").mod(BigInteger.valueOf(97)).intValue();
        return "%s-%02d.%s.%s.%s.%s".formatted(n, dv, ano, justica, tribunal, origem);
    }
}
