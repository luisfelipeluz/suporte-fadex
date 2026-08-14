package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AdicionarComentarioRequest")
public record AdicionarComentarioRequest(
        @Schema(
                example = "Recebido. Vou verificar o fusor ainda hoje pela manhã e retorno com um prazo.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O texto do comentário é obrigatório.")
        @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres.")
        String texto) {}
