package br.org.fadex.chamados.domain;

/**
 * Indica quem definiu a classificacao vigente (categoria + prioridade) do chamado.
 *
 * <p>Permite distinguir na interface a <em>sugestao da IA</em> da <em>classificacao
 * final</em>, conforme exigido pelo desafio.
 */
public enum OrigemClassificacao {

    /** Classificacao gerada pela triagem automatica e ainda nao alterada. */
    IA("Classificação por IA"),

    /** Classificacao definida ou corrigida manualmente pela equipe de suporte. */
    MANUAL("Classificação manual");

    private final String rotulo;

    OrigemClassificacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
