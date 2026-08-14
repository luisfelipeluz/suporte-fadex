package br.org.fadex.chamados.exception;

/** Recurso inexistente. Mapeada para {@code 404 Not Found}. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException chamado(Long id) {
        return new RecursoNaoEncontradoException("Chamado #" + id + " não encontrado.");
    }

    public static RecursoNaoEncontradoException usuario(Long id) {
        return new RecursoNaoEncontradoException("Usuário " + id + " não encontrado.");
    }
}
