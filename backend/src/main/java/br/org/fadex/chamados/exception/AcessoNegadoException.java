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
}
