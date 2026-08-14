package br.org.fadex.chamados.duplicados;

import br.org.fadex.chamados.domain.Chamado;

import java.util.List;

/**
 * Um chamado apontado como possivel duplicata, com o porque da aproximacao.
 *
 * @param chamado chamado ja existente
 * @param score similaridade no intervalo [0,1]
 * @param termosEmComum termos que mais aproximaram os dois textos
 */
public record ChamadoSimilar(Chamado chamado, double score, List<String> termosEmComum) {

    /** Similaridade em pontos percentuais, para exibicao. */
    public int percentual() {
        return (int) Math.round(score * 100);
    }
}
