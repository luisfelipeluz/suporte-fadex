package br.org.fadex.chamados.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Converte excecoes em respostas HTTP consistentes.
 *
 * <p>Centralizar esse mapeamento evita que cada controller precise se preocupar
 * com codigos de status, e garante que toda falha chegue ao cliente no mesmo
 * formato ({@link ApiError}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- 400 Bad Request ------------------------------------------------------

    /** Falha de validacao de DTO anotado com {@code @Valid}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        List<ApiError.CampoInvalido> campos =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> new ApiError.CampoInvalido(e.getField(), e.getDefaultMessage()))
                        .toList();

        return ResponseEntity.badRequest().body(ApiError.deValidacao(req.getRequestURI(), campos));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> tratarConstraint(
            ConstraintViolationException ex, HttpServletRequest req) {

        List<ApiError.CampoInvalido> campos =
                ex.getConstraintViolations().stream()
                        .map(v -> new ApiError.CampoInvalido(
                                v.getPropertyPath().toString(), v.getMessage()))
                        .toList();

        return ResponseEntity.badRequest().body(ApiError.deValidacao(req.getRequestURI(), campos));
    }

    /** JSON malformado ou valor invalido para um enum (ex.: prioridade inexistente). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> tratarCorpoIlegivel(HttpServletRequest req) {
        return badRequest(
                "Corpo da requisição inválido. Verifique o JSON enviado e os valores dos campos.",
                req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tratarTipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {

        return badRequest(
                "O parâmetro '" + ex.getName() + "' recebeu um valor inválido.", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> tratarParametroAusente(
            MissingServletRequestParameterException ex, HttpServletRequest req) {

        return badRequest("O parâmetro '" + ex.getParameterName() + "' é obrigatório.", req);
    }

    // --- 401 Unauthorized -----------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> tratarAutenticacao(
            AuthenticationException ex, HttpServletRequest req) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.de(401, "Unauthorized", "Credenciais inválidas.", req.getRequestURI()));
    }

    // --- 403 Forbidden --------------------------------------------------------

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ApiError> tratarAcessoNegado(
            AcessoNegadoException ex, HttpServletRequest req) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.de(403, "Forbidden", ex.getMessage(), req.getRequestURI()));
    }

    /** Lancada por {@code @PreAuthorize} quando o papel do usuario nao permite a operacao. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> tratarAccessDenied(HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.de(
                        403,
                        "Forbidden",
                        "Você não possui permissão para executar esta operação.",
                        req.getRequestURI()));
    }

    // --- 404 Not Found --------------------------------------------------------

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> tratarNaoEncontrado(
            Exception ex, HttpServletRequest req) {

        String mensagem =
                ex instanceof RecursoNaoEncontradoException
                        ? ex.getMessage()
                        : "Recurso não encontrado.";

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.de(404, "Not Found", mensagem, req.getRequestURI()));
    }

    // --- 409 Conflict ---------------------------------------------------------

    @ExceptionHandler({RegraDeNegocioException.class, EmailJaCadastradoException.class})
    public ResponseEntity<ApiError> tratarRegraDeNegocio(
            RuntimeException ex, HttpServletRequest req) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.de(409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    /**
     * Rede de seguranca para constraints do banco (ex.: e-mail duplicado em uma
     * corrida entre duas requisicoes simultaneas).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> tratarIntegridade(
            DataIntegrityViolationException ex, HttpServletRequest req) {

        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.de(
                        409,
                        "Conflict",
                        "A operação viola uma restrição de integridade dos dados.",
                        req.getRequestURI()));
    }

    // --- 500 Internal Server Error --------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> tratarInesperado(Exception ex, HttpServletRequest req) {
        // O detalhe fica no log do servidor; o cliente recebe apenas uma mensagem
        // generica, para nao expor detalhes internos da aplicacao.
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.de(
                        500,
                        "Internal Server Error",
                        "Ocorreu um erro inesperado. Tente novamente em alguns instantes.",
                        req.getRequestURI()));
    }

    private ResponseEntity<ApiError> badRequest(String mensagem, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiError.de(400, "Bad Request", mensagem, req.getRequestURI()));
    }
}
