package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.OrigemClassificacao;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Representacao enxuta usada nas linhas da listagem e do dashboard. */
@Schema(name = "ChamadoResumo")
public record ChamadoResumoResponse(
        Long id,
        String titulo,
        Categoria categoria,
        @Schema(example = "Hardware") String categoriaRotulo,
        Prioridade prioridade,
        @Schema(example = "ALTA") String prioridadeRotulo,
        StatusChamado status,
        @Schema(example = "Aberto") String statusRotulo,
        UsuarioResponse solicitante,
        UsuarioResponse responsavel,
        OrigemClassificacao origemClassificacao,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static ChamadoResumoResponse de(Chamado chamado) {
        return new ChamadoResumoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getCategoria(),
                chamado.getCategoria().getRotulo(),
                chamado.getPrioridade(),
                chamado.getPrioridade().getRotulo(),
                chamado.getStatus(),
                chamado.getStatus().getRotulo(),
                UsuarioResponse.de(chamado.getSolicitante()),
                UsuarioResponse.de(chamado.getResponsavel()),
                chamado.getOrigemClassificacao(),
                chamado.getCriadoEm(),
                chamado.getAtualizadoEm());
    }
}
