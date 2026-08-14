package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Abertura de chamado.
 *
 * <p>Categoria e prioridade nao fazem parte do payload: quem as define e a
 * triagem automatica. E a premissa central do desafio — o solicitante descreve o
 * problema, o sistema classifica.
 */
@Schema(name = "CriarChamadoRequest", description = "Dados para abrir um chamado")
public record CriarChamadoRequest(
        @Schema(example = "Impressora do 3º andar não está funcionando",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
        String titulo,

        @Schema(
                example =
                        "A impressora do 3º andar não imprime desde ontem. Aparece luz vermelha "
                                + "no painel e o setor inteiro está sem imprimir os empenhos do dia.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(min = 10, max = 4000, message = "A descrição deve ter entre 10 e 4000 caracteres.")
        String descricao) {}
