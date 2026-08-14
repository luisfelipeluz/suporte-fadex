package br.org.fadex.chamados.domain;

/**
 * Grau de confianca da triagem automatica.
 *
 * <p>Nao e exigido pelo desafio, mas e explicitamente permitido e a interface ja
 * preve a exibicao desse indicador junto da sugestao da IA.
 */
public enum Confianca {

    BAIXA("Baixa", 44),
    MEDIA("Média", 68),
    ALTA("Alta", 92);

    private final String rotulo;

    /** Percentual usado pela interface para desenhar a barra de confianca. */
    private final int percentual;

    Confianca(String rotulo, int percentual) {
        this.rotulo = rotulo;
        this.percentual = percentual;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getPercentual() {
        return percentual;
    }
}
