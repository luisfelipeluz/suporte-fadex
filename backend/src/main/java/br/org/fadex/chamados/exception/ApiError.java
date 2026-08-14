package br.org.fadex.chamados.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padrao de toda resposta de erro da API.
 *
 * <p>Formato unico para 400/401/403/404/409/500, para que o frontend possa tratar
 * falhas de maneira uniforme.
 */
@Schema(name = "Erro", description = "Formato padrão das respostas de erro da API")
public record ApiError(
        @Schema(description = "Momento em que o erro ocorreu", example = "2026-08-13T18:25:43.511Z")
        Instant timestamp,

        @Schema(description = "Código HTTP", example = "409")
        int status,

        @Schema(description = "Nome do código HTTP", example = "Conflict")
        String erro,

        @Schema(description = "Mensagem legível para o usuário final",
                example = "Chamados fechados não podem ser reabertos.")
        String mensagem,

        @Schema(description = "Caminho da requisição", example = "/api/chamados/1024/status")
        String caminho,

        @Schema(description = "Erros de validação por campo (presente apenas em 400)")
        List<CampoInvalido> campos) {

    /** Detalhe de uma validacao de campo que falhou. */
    @Schema(name = "CampoInvalido")
    public record CampoInvalido(
            @Schema(example = "titulo") String campo,
            @Schema(example = "O título é obrigatório.") String mensagem) {}

    public static ApiError de(int status, String erro, String mensagem, String caminho) {
        return new ApiError(Instant.now(), status, erro, mensagem, caminho, null);
    }

    public static ApiError deValidacao(String caminho, List<CampoInvalido> campos) {
        return new ApiError(
                Instant.now(),
                400,
                "Bad Request",
                "Há campos inválidos na requisição.",
                caminho,
                campos);
    }
}
