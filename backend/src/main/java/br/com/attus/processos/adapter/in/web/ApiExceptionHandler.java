package br.com.attus.processos.adapter.in.web;

import br.com.attus.processos.domain.exception.ConflitoException;
import br.com.attus.processos.domain.exception.DadoInvalidoException;
import br.com.attus.processos.domain.exception.DominioException;
import br.com.attus.processos.domain.exception.RecursoNaoEncontradoException;
import br.com.attus.processos.domain.exception.RegraNegocioException;
import br.com.attus.processos.shared.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String PROPRIEDADE_ERROS = "erros";

    public record ErroCampo(String campo, String mensagem) {
    }

    @ExceptionHandler(DominioException.class)
    ProblemDetail handleDominio(DominioException ex) {
        var status = statusHttpDe(ex);
        var problema = problema(status, ex.getMessage());
        if (ex instanceof DadoInvalidoException invalido) {
            problema.setProperty(PROPRIEDADE_ERROS, List.of(new ErroCampo(invalido.getCampo(), invalido.getMessage())));
        }
        log.info("Requisicao rejeitada pelo dominio [status={}, motivo={}]", status.value(), ex.getMessage());
        return problema;
    }

    private static HttpStatus statusHttpDe(DominioException ex) {
        return switch (ex) {
            case DadoInvalidoException ignored -> HttpStatus.BAD_REQUEST;
            case RecursoNaoEncontradoException ignored -> HttpStatus.NOT_FOUND;
            case ConflitoException ignored -> HttpStatus.CONFLICT;
            case RegraNegocioException ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleInesperado(Exception ex) {
        log.error("Erro inesperado ao processar requisicao", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno. Informe o correlationId ao suporte.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        var erros = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        var problema = problema(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problema.setProperty(PROPRIEDADE_ERROS, erros);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problema) {
            adicionarCorrelationId(problema);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ProblemDetail problema(HttpStatus status, String detalhe) {
        var problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        adicionarCorrelationId(problema);
        return problema;
    }

    private static void adicionarCorrelationId(ProblemDetail problema) {
        CorrelationId.atual().ifPresent(id -> problema.setProperty(CorrelationId.MDC_KEY, id));
    }
}
