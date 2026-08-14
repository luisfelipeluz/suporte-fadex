package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AtribuirResponsavelRequest")
public record AtribuirResponsavelRequest(
        @Schema(
                description = "Identificador do usuário ADMIN que assumirá o chamado",
                example = "2",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "O responsável é obrigatório.")
        Long responsavelId) {}
