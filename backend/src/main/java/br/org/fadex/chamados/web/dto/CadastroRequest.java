package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro de um novo usuario.
 *
 * <p>O papel nao faz parte do payload: todo cadastro publico cria um SOLICITANTE.
 * Permitir que o proprio usuario escolhesse ser ADMIN seria uma escalacao de
 * privilegio trivial.
 */
@Schema(name = "CadastroRequest", description = "Dados para criar uma conta de solicitante")
public record CadastroRequest(
        @Schema(example = "Marina Castro", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @Schema(example = "marina.castro@fadex.org.br", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres.")
        String email,

        @Schema(example = "suporte123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha) {}
