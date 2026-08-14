package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AutenticacaoResponse", description = "Token emitido e dados do usuário autenticado")
public record AutenticacaoResponse(
        @Schema(description = "Token JWT", example = "eyJhbGciOiJIUzI1NiJ9...") String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(description = "Validade do token em segundos", example = "28800")
        long expiraEmSegundos,
        UsuarioResponse usuario) {

    public static AutenticacaoResponse de(String token, long expiraEmSegundos, UsuarioResponse usuario) {
        return new AutenticacaoResponse(token, "Bearer", expiraEmSegundos, usuario);
    }
}
