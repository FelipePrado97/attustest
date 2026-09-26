package br.com.attus.processos.adapter.in.web;

import br.com.attus.processos.shared.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var correlationId = CorrelationId.aceitarOuGerar(request.getHeader(CorrelationId.HEADER));
        CorrelationId.definir(correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        var inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            var duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), duracaoMs);
            CorrelationId.limpar();
        }
    }
}
