package br.org.fadex.chamados.triage;

/**
 * Texto submetido a triagem automatica.
 *
 * <p>Intencionalmente desacoplado da entidade {@code Chamado}: o provider recebe
 * apenas o texto de que precisa, e nao o modelo de dominio inteiro.
 */
public record TriageRequest(String titulo, String descricao) {

    public TriageRequest {
        titulo = titulo == null ? "" : titulo.trim();
        descricao = descricao == null ? "" : descricao.trim();
    }
}
