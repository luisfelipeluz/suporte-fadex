package br.org.fadex.chamados.security;

import br.org.fadex.chamados.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Devolve 401 e 403 no mesmo formato {@link ApiError} usado pelo resto da API.
 *
 * <p>Sem isso, falhas de autenticacao e autorizacao interceptadas pela cadeia de
 * filtros retornariam o corpo padrao do container, diferente do restante das
 * respostas de erro — e o frontend teria de tratar dois formatos distintos.
 */
@Component
public class TratadorErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public TratadorErroSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Requisicao sem token valido para um recurso protegido. */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        escrever(
                request,
                response,
                401,
                "Unauthorized",
                "Autenticação necessária. Envie um token válido no cabeçalho Authorization.");
    }

    /** Usuario autenticado, mas sem permissao para o recurso. */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {

        escrever(
                request,
                response,
                403,
                "Forbidden",
                "Você não possui permissão para acessar este recurso.");
    }

    private void escrever(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String erro,
            String mensagem)
            throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiError corpo = ApiError.de(status, erro, mensagem, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
