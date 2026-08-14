package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credenciais de acesso")
public record LoginRequest(
        @Schema(example = "ana.souza@fadex.org.br", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O e-mail é obrigatório.")
        String email,

        @Schema(example = "suporte123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "A senha é obrigatória.")
        String senha) {}
