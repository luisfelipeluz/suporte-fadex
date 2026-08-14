package br.org.fadex.chamados.domain;

/**
 * Categorias de chamado.
 *
 * <p>O documento oficial do desafio nao fixa uma lista de categorias. Optou-se por
 * um enum — em vez de uma tabela com CRUD proprio, que ampliaria o escopo sem
 * necessidade — exposto pelo endpoint {@code GET /api/categorias}, de modo que a
 * interface consome a lista da API em vez de codifica-la.
 */
public enum Categoria {

    HARDWARE("Hardware"),
    SOFTWARE("Software"),
    ACESSO("Acesso"),
    REDE("Rede"),
    SISTEMAS("Sistemas"),
    OUTROS("Outros");

    private final String rotulo;

    Categoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
