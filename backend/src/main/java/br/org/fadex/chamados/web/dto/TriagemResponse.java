package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.Confianca;
import br.org.fadex.chamados.domain.OrigemClassificacao;
import br.org.fadex.chamados.domain.Prioridade;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bloco de triagem exibido no detalhe do chamado.
 *
 * <p>Devolve a sugestao da IA e a classificacao final lado a lado, de modo que a
 * interface possa deixar explicito quando o suporte discordou da IA.
 */
@Schema(name = "Triagem", description = "Sugestão da IA versus classificação final")
public record TriagemResponse(
        @Schema(description = "Categoria sugerida pela IA", example = "HARDWARE")
        Categoria categoriaSugerida,

        @Schema(description = "Prioridade sugerida pela IA", example = "ALTA")
        Prioridade prioridadeSugerida,

        @Schema(description = "Categoria final vigente", example = "HARDWARE")
        Categoria categoriaFinal,

        @Schema(description = "Prioridade final vigente", example = "ALTA")
        Prioridade prioridadeFinal,

        @Schema(description = "Confiança da sugestão", example = "ALTA") Confianca confianca,

        @Schema(description = "Percentual de confiança para a barra da interface", example = "92")
        Integer confiancaPercentual,

        @Schema(description = "Explicação legível da classificação")
        String justificativa,

        @Schema(description = "Origem da classificação vigente", example = "IA")
        OrigemClassificacao origem,

        @Schema(description = "Provider que gerou a sugestão", example = "heuristic")
        String provedor,

        @Schema(description = "Indica se o ADMIN já revisou a sugestão") boolean revisada,

        @Schema(description = "Indica se a classificação final difere da sugerida")
        boolean divergente) {

    public static TriagemResponse de(Chamado chamado) {
        Confianca confianca = chamado.getConfiancaIa();

        return new TriagemResponse(
                chamado.getCategoriaSugerida(),
                chamado.getPrioridadeSugerida(),
                chamado.getCategoria(),
                chamado.getPrioridade(),
                confianca,
                confianca == null ? null : confianca.getPercentual(),
                chamado.getJustificativaIa(),
                chamado.getOrigemClassificacao(),
                chamado.getProvedorTriagem(),
                chamado.isClassificacaoRevisada(),
                chamado.isClassificacaoDivergente());
    }
}
