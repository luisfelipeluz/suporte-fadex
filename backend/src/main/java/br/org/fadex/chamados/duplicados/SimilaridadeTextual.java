package br.org.fadex.chamados.duplicados;

import br.org.fadex.chamados.texto.Texto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Similaridade entre dois textos de chamado, por cosseno sobre termos ponderados.
 *
 * <h2>Por que assim</h2>
 *
 * <p>O desafio lista a deteccao de duplicados como diferencial e sugere
 * "similaridade de texto/embeddings". A escolha aqui segue a mesma logica da
 * triagem: um metodo <b>deterministico e explicavel</b>, que roda sem rede e sem
 * chave de API, e que por isso pode ser testado de verdade. Embeddings dariam mais
 * recall em parafrases, ao custo de uma dependencia externa em um caminho que roda
 * a cada abertura de chamado — e sem ganho para o caso que realmente importa
 * aqui, que e o mesmo incidente relatado por varias pessoas com praticamente as
 * mesmas palavras ("impressora do financeiro nao imprime").
 *
 * <h2>Como funciona</h2>
 *
 * <ol>
 *   <li>titulo e descricao viram termos normalizados, sem stopwords (ver {@link Texto});
 *   <li>cada termo recebe um peso: ocorrencias no titulo pesam mais, porque o
 *       titulo tende a nomear o incidente enquanto a descricao o contextualiza —
 *       a mesma ponderacao usada pela triagem heuristica;
 *   <li>os dois chamados viram vetores nesse espaco de termos e a similaridade e o
 *       cosseno entre eles: 1,0 e texto equivalente, 0,0 e nenhum termo em comum.
 * </ol>
 *
 * <p>O cosseno foi preferido ao Jaccard porque normaliza pelo tamanho: um chamado
 * curto e um chamado longo que descrevem o mesmo problema continuam proximos, em
 * vez de serem penalizados pela diferenca de extensao.
 */
public final class SimilaridadeTextual {

    /** Um termo no titulo pesa mais do que o mesmo termo perdido na descricao. */
    private static final int PESO_TITULO = 3;

    private static final int PESO_DESCRICAO = 1;

    private SimilaridadeTextual() {
        // utilitario
    }

    /** Converte titulo e descricao no vetor de termos usado na comparacao. */
    public static Perfil perfilar(String titulo, String descricao) {
        Map<String, Integer> pesos = new HashMap<>();

        for (String termo : Texto.tokenizar(titulo)) {
            pesos.merge(termo, PESO_TITULO, Integer::sum);
        }
        for (String termo : Texto.tokenizar(descricao)) {
            pesos.merge(termo, PESO_DESCRICAO, Integer::sum);
        }

        double soma = pesos.values().stream().mapToDouble(p -> (double) p * p).sum();

        return new Perfil(Map.copyOf(pesos), Math.sqrt(soma));
    }

    /**
     * Compara dois perfis.
     *
     * <p>Devolve tambem os termos responsaveis pela aproximacao, para que a
     * interface possa justificar ao usuario por que dois chamados foram
     * considerados parecidos — o mesmo principio da justificativa da triagem.
     */
    public static Comparacao comparar(Perfil a, Perfil b) {
        if (a.vazio() || b.vazio()) {
            return new Comparacao(0.0, List.of());
        }

        // Itera sobre o menor dos dois mapas: o produto interno so tem contribuicao
        // nos termos presentes em ambos.
        Map<String, Integer> menor = a.pesos().size() <= b.pesos().size() ? a.pesos() : b.pesos();
        Map<String, Integer> maior = menor == a.pesos() ? b.pesos() : a.pesos();

        double produto = 0.0;
        Map<String, Integer> contribuicoes = new HashMap<>();

        for (Map.Entry<String, Integer> entrada : menor.entrySet()) {
            Integer pesoNoOutro = maior.get(entrada.getKey());
            if (pesoNoOutro != null) {
                int contribuicao = entrada.getValue() * pesoNoOutro;
                produto += contribuicao;
                contribuicoes.put(entrada.getKey(), contribuicao);
            }
        }

        if (produto == 0.0) {
            return new Comparacao(0.0, List.of());
        }

        double score = produto / (a.norma() * b.norma());

        List<String> termos =
                contribuicoes.entrySet().stream()
                        .sorted(
                                Map.Entry.<String, Integer>comparingByValue()
                                        .reversed()
                                        .thenComparing(Map.Entry.comparingByKey()))
                        .limit(5)
                        .map(Map.Entry::getKey)
                        .toList();

        // O cosseno pode passar de 1,0 por erro de ponto flutuante; limita para que
        // o percentual exibido na interface nunca seja "101%".
        return new Comparacao(Math.min(1.0, score), termos);
    }

    /** Vetor de termos de um chamado, com a norma ja calculada. */
    public record Perfil(Map<String, Integer> pesos, double norma) {

        public boolean vazio() {
            return pesos.isEmpty() || norma == 0.0;
        }
    }

    /**
     * @param score similaridade no intervalo [0,1]
     * @param termosEmComum ate cinco termos que mais aproximaram os textos
     */
    public record Comparacao(double score, List<String> termosEmComum) {}
}
