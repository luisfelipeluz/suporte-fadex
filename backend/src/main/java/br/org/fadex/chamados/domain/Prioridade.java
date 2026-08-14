package br.org.fadex.chamados.domain;

/**
 * Prioridade de atendimento do chamado.
 *
 * <p>Os nomes das constantes sao mantidos em ASCII para que os valores trafeguem
 * de forma estavel na API e no banco; o texto acentuado exibido na interface vem
 * de {@link #getRotulo()}.
 */
public enum Prioridade {

    BAIXA("BAIXA"),
    MEDIA("MÉDIA"),
    ALTA("ALTA");

    private final String rotulo;

    Prioridade(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Chamados ALTA disparam alerta imediato para a equipe ADMIN. */
    public boolean exigeAlertaImediato() {
        return this == ALTA;
    }
}
