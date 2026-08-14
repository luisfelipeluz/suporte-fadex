package br.org.fadex.chamados.triage;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Confianca;
import br.org.fadex.chamados.domain.Prioridade;

/**
 * Resultado de uma triagem.
 *
 * @param categoria categoria sugerida
 * @param prioridade prioridade sugerida
 * @param confianca grau de confianca da sugestao
 * @param justificativa explicacao legivel do porque da classificacao, exibida na interface
 * @param provedor identificacao de quem classificou (ex.: {@code heuristic}, {@code gemini})
 */
public record TriageResult(
        Categoria categoria,
        Prioridade prioridade,
        Confianca confianca,
        String justificativa,
        String provedor) {}
