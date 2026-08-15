package br.org.fadex.chamados.exception;

/**
 * Usuario autenticado tentando acessar recurso ou operacao fora do seu perfil.
 *
 * <p>Mapeada para {@code 403 Forbidden}.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }

    public static AcessoNegadoException chamadoDeOutroUsuario() {
        return new AcessoNegadoException("Você não possui permissão para acessar este chamado.");
    }

    public static AcessoNegadoException exclusivoAdmin(String operacao) {
        return new AcessoNegadoException("Apenas a equipe administrativa pode " + operacao + ".");
    }

    /**
     * Solicitante tentando uma mudanca de status que nao lhe cabe.
     *
     * <p>Ele so age no proprio chamado, e apenas quando o atendimento devolve algo
     * para ele avaliar: confirmar a resolucao ou dizer que o problema continua.
     */
    public static AcessoNegadoException mudancaDeStatusRestrita() {
        return new AcessoNegadoException(
                "Você só pode confirmar a resolução ou reabrir o atendimento de um chamado seu que "
                        + "esteja resolvido. As demais mudanças de status são feitas pela equipe de suporte.");
    }
}
