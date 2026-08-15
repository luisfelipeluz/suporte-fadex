package br.org.fadex.chamados.repository;

/**
 * Quantos chamados cada mecanismo de triagem classificou.
 *
 * <p>Existe como projecao propria porque o provedor nao e um enum, e sim o nome
 * do mecanismo que produziu a sugestao ({@code heuristic}, {@code gemini}). Como
 * esse conjunto muda sem alterar o codigo — basta configurar outro provider — a
 * contagem e agrupada na consulta, em vez de enumerada como e feito com status,
 * prioridade e origem.
 */
public record ContagemPorProvedor(String provedor, Long total) {}
