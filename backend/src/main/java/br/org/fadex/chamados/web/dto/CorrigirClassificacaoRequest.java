package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Prioridade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Correcao manual da classificacao sugerida pela IA. Exclusiva do ADMIN. */
@Schema(name = "CorrigirClassificacaoRequest")
public record CorrigirClassificacaoRequest(
        @Schema(example = "HARDWARE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "A categoria é obrigatória.")
        Categoria categoria,

        @Schema(example = "ALTA", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "A prioridade é obrigatória.")
        Prioridade prioridade) {}
