package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.duplicados.ChamadoSimilar;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Chamado apontado como possivel duplicata.
 *
 * <p>Carrega o motivo da aproximacao, e nao apenas o percentual: a interface
 * precisa conseguir justificar a sugestao para quem vai decidir se realmente e o
 * mesmo incidente.
 */
@Schema(name = "ChamadoSimilar")
public record ChamadoSimilarResponse(
        @Schema(example = "42") Long id,
        @Schema(example = "Impressora do financeiro não imprime") String titulo,
        StatusChamado status,
        @Schema(example = "Em andamento") String statusRotulo,
        @Schema(description = "Cor do status, usada nos badges", example = "#b45309")
        String statusCor,
        @Schema(description = "Quem abriu o chamado semelhante") UsuarioResponse solicitante,
        Instant criadoEm,
        @Schema(description = "Similaridade textual em pontos percentuais", example = "78")
        int similaridade,
        @Schema(description = "Termos que aproximaram os dois chamados", example = "[\"impressora\", \"financeiro\"]")
        List<String> termosEmComum) {

    public static ChamadoSimilarResponse de(ChamadoSimilar similar) {
        Chamado chamado = similar.chamado();

        return new ChamadoSimilarResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getStatus(),
                chamado.getStatus().getRotulo(),
                chamado.getStatus().getCor(),
                UsuarioResponse.de(chamado.getSolicitante()),
                chamado.getCriadoEm(),
                similar.percentual(),
                similar.termosEmComum());
    }
}
