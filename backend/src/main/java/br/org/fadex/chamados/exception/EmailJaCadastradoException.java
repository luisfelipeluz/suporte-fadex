package br.org.fadex.chamados.exception;

/**
 * Tentativa de cadastrar um e-mail ja existente.
 *
 * <p>Mapeada para {@code 409 Conflict}. A unicidade tambem e garantida por
 * constraint no banco, de modo que a regra nao depende apenas da verificacao
 * previa em codigo.
 */
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário cadastrado com o e-mail " + email + ".");
    }
}
