package br.org.fadex.chamados.triage;

/**
 * Contrato de um mecanismo de classificacao automatica.
 *
 * <p>Esta abstracao e o ponto central do requisito de arquitetura do desafio: o
 * dominio depende apenas desta interface, nunca de um fornecedor de IA concreto.
 * Trocar a heuristica local por uma API externa — ou vice-versa — e questao de
 * configuracao, sem alterar {@code ChamadoService} nem a entidade {@code Chamado}.
 *
 * @see HeuristicTriageProvider implementacao padrao, deterministica e offline
 */
public interface TriageProvider {

    /**
     * Classifica o texto informado.
     *
     * <p>Implementacoes nao devem lancar excecao para entradas vazias ou sem
     * padrao reconhecivel: nesses casos devem devolver uma classificacao neutra
     * com confianca baixa, pois a abertura do chamado nao pode falhar por causa
     * da triagem.
     */
    TriageResult classificar(TriageRequest requisicao);

    /** Identificador usado na configuracao e gravado junto do chamado. */
    String nome();
}
