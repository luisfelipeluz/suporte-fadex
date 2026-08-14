package br.org.fadex.chamados.repository;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.domain.Usuario;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Filtros compostaveis da listagem de chamados.
 *
 * <p>Cada filtro e uma {@link Specification} independente que devolve {@code null}
 * quando o parametro nao foi informado — assim o servico combina apenas os filtros
 * ativos, sem montar SQL condicional na mao.
 *
 * <p>O filtro {@link #doSolicitante(Usuario)} e o que garante, na propria consulta,
 * que um SOLICITANTE jamais receba chamados de terceiros.
 */
public final class ChamadoSpecifications {

    private ChamadoSpecifications() {}

    public static Specification<Chamado> comStatus(StatusChamado status) {
        return status == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get("status"), status);
    }

    public static Specification<Chamado> comPrioridade(Prioridade prioridade) {
        return prioridade == null
                ? null
                : (raiz, consulta, cb) -> cb.equal(raiz.get("prioridade"), prioridade);
    }

    public static Specification<Chamado> comCategoria(Categoria categoria) {
        return categoria == null
                ? null
                : (raiz, consulta, cb) -> cb.equal(raiz.get("categoria"), categoria);
    }

    /** Restringe a listagem aos chamados abertos pelo usuario informado. */
    public static Specification<Chamado> doSolicitante(Usuario solicitante) {
        return solicitante == null
                ? null
                : (raiz, consulta, cb) -> cb.equal(raiz.get("solicitante"), solicitante);
    }

    /** Busca textual por titulo, descricao ou numero do chamado. */
    public static Specification<Chamado> comTexto(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String padrao = "%" + termo.trim().toLowerCase(Locale.ROOT) + "%";

        return (raiz, consulta, cb) -> {
            var porTitulo = cb.like(cb.lower(raiz.get("titulo")), padrao);
            var porDescricao = cb.like(cb.lower(raiz.get("descricao")), padrao);
            var porId = cb.like(cb.lower(raiz.get("id").as(String.class)), padrao);
            return cb.or(porTitulo, porDescricao, porId);
        };
    }
}
