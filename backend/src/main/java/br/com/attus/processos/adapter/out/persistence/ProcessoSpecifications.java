package br.com.attus.processos.adapter.out.persistence;

import br.com.attus.processos.application.port.FiltroProcessos;
import br.com.attus.processos.domain.model.NumeroCnj;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

final class ProcessoSpecifications {

    private static final char ESCAPE = '\\';

    private static final Pattern TERMO_NUMERICO = Pattern.compile("[\\d.\\-\\s]+");

    private ProcessoSpecifications() {
    }

    static Specification<ProcessoJpaEntity> de(FiltroProcessos filtro) {
        return (root, query, cb) -> {
            var predicados = new ArrayList<Predicate>();
            if (filtro.status() != null) {
                predicados.add(cb.equal(root.get("status"), filtro.status()));
            }
            if (filtro.termo() != null && !filtro.termo().isBlank()) {
                predicados.add(buscaLivre(filtro.termo().strip(), root, cb));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static Predicate buscaLivre(String termo, Root<ProcessoJpaEntity> root, CriteriaBuilder cb) {
        var like = contem(termo.toLowerCase(Locale.ROOT));
        var criterios = new ArrayList<Predicate>();
        criterios.add(cb.like(cb.lower(root.get("assunto")), like, ESCAPE));
        criterios.add(cb.like(cb.lower(root.get("parteContraria")), like, ESCAPE));
        if (TERMO_NUMERICO.matcher(termo).matches()) {
            var digitos = NumeroCnj.somenteDigitos(termo);
            if (!digitos.isEmpty()) {
                criterios.add(cb.like(root.get("numeroCnjDigitos"), contem(digitos), ESCAPE));
            }
        }
        return cb.or(criterios.toArray(Predicate[]::new));
    }

    private static String contem(String termo) {
        return "%" + escaparLike(termo) + "%";
    }

    private static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
