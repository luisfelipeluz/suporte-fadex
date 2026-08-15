package br.org.fadex.chamados.domain;

import java.util.Optional;

/**
 * Estados do ciclo de vida de um chamado.
 *
 * <p>O fluxo previsto e sequencial, e caminha nos dois sentidos:
 *
 * <pre>
 *   ABERTO &lt;-> EM_ANDAMENTO &lt;-> RESOLVIDO -> FECHADO
 *      |               |              |
 *      +---------------+--------------+---> FECHADO  (encerramento direto)
 * </pre>
 *
 * <p>O retrocesso existe porque "resolvido" e uma afirmacao que pode se provar
 * falsa: quando o atendimento nao resolveu o que foi pedido, o chamado volta uma
 * etapa em vez de ser fechado ou duplicado. O movimento e sempre de uma etapa por
 * vez, em qualquer direcao — nao ha salto.
 *
 * <p>{@link #FECHADO} e {@link #CANCELADO} sao estados terminais. A regra de negocio
 * "chamado fechado nao pode ser reaberto" e expressa aqui, em um unico lugar, e
 * aplicada pelo servico de dominio — nunca apenas escondendo um botao na interface.
 * E por isso que {@link #FECHADO} nao tem etapa anterior: o retorno para
 * {@link #RESOLVIDO} seria exatamente a reabertura que a regra proibe.
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
     * Estado imediatamente anterior do fluxo, para onde o chamado pode retornar.
     *
     * <p>Vazio na primeira etapa e nos estados terminais: de {@link #FECHADO} nao
     * ha volta, e {@link #CANCELADO} e saida do fluxo, nao uma etapa dele.
     */
    public Optional<StatusChamado> anterior() {
        return switch (this) {
            case EM_ANDAMENTO -> Optional.of(ABERTO);
            case RESOLVIDO -> Optional.of(EM_ANDAMENTO);
            case ABERTO, FECHADO, CANCELADO -> Optional.empty();
        };
    }

    /**
     * Indica se a transicao deste estado para {@code destino} e valida.
     *
     * <p>Vale o avanco para a proxima etapa, o retorno para a anterior e o
     * encerramento direto (ver {@link #permiteFechamentoDireto()}). O que continua
     * proibido: saltar etapas no meio do fluxo, reabrir chamado encerrado e mexer
     * em chamado cancelado.
     */
    public boolean permiteTransicaoPara(StatusChamado destino) {
        if (destino == null || isEncerrado()) {
            return false;
        }
        if (destino == FECHADO) {
            return permiteFechamentoDireto();
        }
        return proximo().filter(destino::equals).isPresent()
                || anterior().filter(destino::equals).isPresent();
    }

    /**
     * Indica se o chamado pode ser encerrado direto, sem percorrer as etapas.
     *
     * <p>E a valvula de escape da operacao: chamado aberto por engano, duplicata
     * ja tratada em outro registro, solicitacao que perdeu o objeto. Obrigar essas
     * situacoes a passar por "em andamento" e "resolvido" so produziria historico
     * falso.
     *
     * <p>Nao ha conflito com a regra de nao reabrir: fechar continua sendo um
     * caminho de ida, e nenhum estado terminal volta atras.
     */
    public boolean permiteFechamentoDireto() {
        return !isEncerrado();
    }

    /**
     * Indica se ir para {@code destino} e um retorno no fluxo.
     *
     * <p>Distingue as duas direcoes para que o historico registre o retorno como
     * o evento que ele e, e nao como mais um avanco de status.
     */
    public boolean isRetrocessoPara(StatusChamado destino) {
        return destino != null && anterior().filter(destino::equals).isPresent();
    }
}
