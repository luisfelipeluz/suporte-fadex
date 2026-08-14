package br.org.fadex.chamados.domain;

import java.util.Optional;

/**
 * Estados do ciclo de vida de um chamado.
 *
 * <p>O fluxo previsto e estritamente sequencial:
 *
 * <pre>
 *   ABERTO -> EM_ANDAMENTO -> RESOLVIDO -> FECHADO
 * </pre>
 *
 * <p>{@link #FECHADO} e {@link #CANCELADO} sao estados terminais. A regra de negocio
 * "chamado fechado nao pode ser reaberto" e expressa aqui, em um unico lugar, e
 * aplicada pelo servico de dominio — nunca apenas escondendo um botao na interface.
 */
public enum StatusChamado {

    ABERTO("Aberto", "#2563eb"),
    EM_ANDAMENTO("Em andamento", "#b45309"),
    RESOLVIDO("Resolvido", "#0f766e"),
    FECHADO("Fechado", "#94a3b8"),

    /**
     * Resultado do cancelamento (exclusao logica). Preserva o chamado e todo o seu
     * historico, em vez de apaga-lo fisicamente do banco.
     */
    CANCELADO("Cancelado", "#94a3b8");

    private final String rotulo;

    /** Cor usada pela interface nos badges e na timeline. */
    private final String cor;

    StatusChamado(String rotulo, String cor) {
        this.rotulo = rotulo;
        this.cor = cor;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getCor() {
        return cor;
    }

    /** Estados terminais nao admitem nenhuma transicao de saida. */
    public boolean isEncerrado() {
        return this == FECHADO || this == CANCELADO;
    }

    /** Um chamado so pode ser cancelado enquanto nao estiver encerrado. */
    public boolean permiteCancelamento() {
        return !isEncerrado();
    }

    /** Proximo estado do fluxo, ou vazio se este for terminal. */
    public Optional<StatusChamado> proximo() {
        return switch (this) {
            case ABERTO -> Optional.of(EM_ANDAMENTO);
            case EM_ANDAMENTO -> Optional.of(RESOLVIDO);
            case RESOLVIDO -> Optional.of(FECHADO);
            case FECHADO, CANCELADO -> Optional.empty();
        };
    }

    /**
     * Indica se a transicao deste estado para {@code destino} e valida.
     *
     * <p>Somente o avanco para o proximo estado do fluxo e permitido: nao ha
     * retrocesso, nem salto de etapas, nem reabertura de chamado encerrado.
     */
    public boolean permiteTransicaoPara(StatusChamado destino) {
        return destino != null && proximo().filter(destino::equals).isPresent();
    }
}
