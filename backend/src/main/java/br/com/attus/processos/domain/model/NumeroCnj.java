package br.com.attus.processos.domain.model;

import br.com.attus.processos.domain.exception.DadoInvalidoException;

import java.math.BigInteger;
import java.util.regex.Pattern;

public record NumeroCnj(String valor) {

    public static final int TAMANHO_FORMATADO = 25;

    private static final String CAMPO = "numeroCnj";
    private static final int TOTAL_DIGITOS = 20;
    private static final BigInteger NOVENTA_E_SETE = BigInteger.valueOf(97);
    private static final Pattern FORMATADO = Pattern.compile("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");
    private static final Pattern NAO_DIGITO = Pattern.compile("\\D");

    public NumeroCnj {
        if (valor == null || !FORMATADO.matcher(valor).matches()) {
            throw new DadoInvalidoException(CAMPO, "Número CNJ deve estar no formato NNNNNNN-DD.AAAA.J.TR.OOOO");
        }
        if (!digitoVerificadorValido(somenteDigitos(valor))) {
            throw new DadoInvalidoException(CAMPO, "Número CNJ com dígito verificador inválido");
        }
    }

    public static NumeroCnj of(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new DadoInvalidoException(CAMPO, "Número CNJ é obrigatório");
        }
        var digitos = somenteDigitos(entrada);
        if (digitos.length() != TOTAL_DIGITOS) {
            throw new DadoInvalidoException(CAMPO, "Número CNJ deve conter 20 dígitos");
        }
        return new NumeroCnj(formatar(digitos));
    }

    public int anoAjuizamento() {
        return Integer.parseInt(valor.substring(11, 15));
    }

    public static String somenteDigitos(String entrada) {
        return NAO_DIGITO.matcher(entrada).replaceAll("");
    }

    private static boolean digitoVerificadorValido(String digitos) {
        var sequencial = digitos.substring(0, 7);
        var dv = digitos.substring(7, 9);
        var restante = digitos.substring(9);
        return new BigInteger(sequencial + restante + dv).mod(NOVENTA_E_SETE).intValue() == 1;
    }

    private static String formatar(String d) {
        return "%s-%s.%s.%s.%s.%s".formatted(
                d.substring(0, 7), d.substring(7, 9), d.substring(9, 13),
                d.substring(13, 14), d.substring(14, 16), d.substring(16, 20));
    }

    @Override
    public String toString() {
        return valor;
    }
}
