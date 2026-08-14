package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.StatusChamado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AlterarStatusRequest")
public record AlterarStatusRequest(
        @Schema(
                description =
                        "Novo status. O fluxo é sequencial: ABERTO → EM_ANDAMENTO → RESOLVIDO → FECHADO.",
                example = "EM_ANDAMENTO",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "O status é obrigatório.")
        StatusChamado status) {}
