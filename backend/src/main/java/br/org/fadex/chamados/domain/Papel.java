package br.org.fadex.chamados.domain;

/** Perfis de acesso do sistema. */
public enum Papel {

    /** Equipe de suporte: enxerga e gerencia todos os chamados. */
    ADMIN("Administrador"),

    /** Usuario interno: abre e acompanha apenas os proprios chamados. */
    SOLICITANTE("Solicitante");

    private final String rotulo;

    Papel(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Nome da authority usada pelo Spring Security (prefixo {@code ROLE_}). */
    public String authority() {
        return "ROLE_" + name();
    }
}
