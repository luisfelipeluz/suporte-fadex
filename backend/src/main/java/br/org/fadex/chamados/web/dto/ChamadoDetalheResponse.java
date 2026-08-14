package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Chamado completo, com triagem, historico e comentarios. */
@Schema(name = "ChamadoDetalhe")
public record ChamadoDetalheResponse(
        Long id,
        String titulo,
        String descricao,
        Categoria categoria,
        String categoriaRotulo,
        Prioridade prioridade,
        String prioridadeRotulo,
        StatusChamado status,
        String statusRotulo,
        UsuarioResponse solicitante,
        UsuarioResponse responsavel,
        Instant criadoEm,
        Instant atualizadoEm,

        @Schema(description = "Sugestão da IA e classificação final") TriagemResponse triagem,

        @Schema(description = "Próximo status permitido pelo fluxo; nulo se encerrado")
        StatusChamado proximoStatus,

        @Schema(description = "Indica que o chamado está encerrado e não admite mais transições")
        boolean encerrado,

        @Schema(description = "Histórico em ordem cronológica")
        List<EventoHistoricoResponse> historico,

        @Schema(description = "Comentários em ordem cronológica")
        List<ComentarioResponse> comentarios,

        @Schema(
                description =
                        "Chamados que possivelmente relatam o mesmo incidente, do mais parecido "
                                + "para o menos. Vazio quando nada semelhante foi encontrado.")
        List<ChamadoSimilarResponse> possiveisDuplicados) {

    public static ChamadoDetalheResponse de(
            Chamado chamado,
            List<EventoHistoricoResponse> historico,
            List<ComentarioResponse> comentarios,
            List<ChamadoSimilarResponse> possiveisDuplicados) {

        return new ChamadoDetalheResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getCategoria(),
                chamado.getCategoria().getRotulo(),
                chamado.getPrioridade(),
                chamado.getPrioridade().getRotulo(),
                chamado.getStatus(),
                chamado.getStatus().getRotulo(),
                UsuarioResponse.de(chamado.getSolicitante()),
                UsuarioResponse.de(chamado.getResponsavel()),
                chamado.getCriadoEm(),
                chamado.getAtualizadoEm(),
                TriagemResponse.de(chamado),
                chamado.getStatus().proximo().orElse(null),
                chamado.getStatus().isEncerrado(),
                historico,
                comentarios,
                possiveisDuplicados);
    }
}
