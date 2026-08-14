package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AtualizarChamadoRequest", description = "Edição do conteúdo de um chamado")
public record AtualizarChamadoRequest(
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(min = 10, max = 4000, message = "A descrição deve ter entre 10 e 4000 caracteres.")
        String descricao) {}
