package br.org.fadex.chamados.exception;

/**
 * Violacao de uma regra de negocio do dominio.
 *
 * <p>Mapeada para {@code 409 Conflict}: a requisicao esta bem formada e o usuario
 * esta autorizado, mas o estado atual do recurso nao permite a operacao.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
