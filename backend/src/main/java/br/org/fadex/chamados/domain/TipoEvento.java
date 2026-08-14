package br.org.fadex.chamados.domain;

/**
 * Tipos de evento registrados no historico de um chamado.
 *
 * <p>A cor acompanha o evento para que a timeline da interface seja renderizada a
 * partir dos dados da API, sem replicar no frontend a semantica de cada evento.
 */
public enum TipoEvento {

    CHAMADO_ABERTO("#2563eb"),
    CLASSIFICACAO_IA("#2563eb"),
    CLASSIFICACAO_ACEITA("#0f766e"),
    CLASSIFICACAO_CORRIGIDA("#b45309"),
    RESPONSAVEL_ATRIBUIDO("#64748b"),
    STATUS_ALTERADO("#2563eb"),
    COMENTARIO_ADICIONADO("#64748b"),
    CHAMADO_ATUALIZADO("#64748b"),
    CHAMADO_CANCELADO("#94a3b8");

    private final String cor;

    TipoEvento(String cor) {
        this.cor = cor;
    }

    public String getCor() {
        return cor;
    }
}
