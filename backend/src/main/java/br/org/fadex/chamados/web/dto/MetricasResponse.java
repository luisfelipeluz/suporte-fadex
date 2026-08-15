package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.OrigemClassificacao;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Indicadores do dashboard.
 *
 * <p>O escopo acompanha o papel de quem consulta: ADMIN recebe os numeros de toda
 * a operacao, SOLICITANTE apenas os dos proprios chamados.
 */
@Schema(name = "Metricas", description = "Indicadores operacionais da central")
public record MetricasResponse(
        @Schema(description = "Total de chamados no escopo do usuário", example = "42")
        long total,

        @Schema(description = "Quantidade por status") Map<StatusChamado, Long> porStatus,

        @Schema(description = "Quantidade por prioridade") Map<Prioridade, Long> porPrioridade,

        @Schema(description = "Quantidade por origem da classificação")
        Map<OrigemClassificacao, Long> porOrigem,

        @Schema(
                description =
                        "Quantidade por mecanismo que produziu a triagem (`heuristic`, `gemini`). "
                                + "Diferente de `porOrigem`, que separa apenas automático de manual: "
                                + "um chamado classificado pela heurística local também conta como "
                                + "origem IA. É aqui que se vê quanto da triagem passou de fato por "
                                + "um modelo externo.",
                example = "{\"gemini\": 18, \"heuristic\": 24}")
        Map<String, Long> porProvedorTriagem,

        @Schema(description = "Chamados de prioridade ALTA ainda não encerrados", example = "5")
        long altaPrioridadeEmAberto,

        @Schema(
                description =
                        "Percentual de chamados classificados automaticamente, por qualquer "
                                + "provider de triagem — inclui a heurística local. Para saber "
                                + "quanto veio de IA externa, use `porProvedorTriagem`.",
                example = "83")
        int percentualClassificadoPorIa,

        @Schema(description = "Momento do cálculo") Instant atualizadoEm) {}
