package br.org.fadex.chamados.realtime;

/**
 * Nomes dos eventos SSE.
 *
 * <p>O frontend registra um listener por nome, entao estes valores fazem parte do
 * contrato publico da API tanto quanto os endpoints REST.
 */
public enum TipoEventoRealtime {

    /** Indicadores recalculados; o dashboard atualiza os KPIs. */
    METRICAS("metricas"),

    /** Um novo chamado entrou na fila. */
    CHAMADO_CRIADO("chamado-criado"),

    /** Um chamado existente mudou (status, responsavel, classificacao, comentario). */
    CHAMADO_ATUALIZADO("chamado-atualizado"),

    /** Chamado de prioridade ALTA aberto: dispara o alerta visual da equipe. */
    ALERTA_ALTA("alerta-alta"),

    /** Primeiro evento apos a conexao, confirmando que o canal esta ativo. */
    CONECTADO("conectado");

    private final String nome;

    TipoEventoRealtime(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
