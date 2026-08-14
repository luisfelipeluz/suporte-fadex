package br.org.fadex.chamados.repository;

import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.OrigemClassificacao;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChamadoRepository
        extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {

    /**
     * Carrega solicitante e responsavel junto do chamado.
     *
     * <p>Sem o {@code EntityGraph}, renderizar uma pagina de 20 chamados dispararia
     * dezenas de consultas adicionais para resolver os relacionamentos LAZY.
     */
    @Override
    @EntityGraph(attributePaths = {"solicitante", "responsavel"})
    Page<Chamado> findAll(org.springframework.data.jpa.domain.Specification<Chamado> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"solicitante", "responsavel"})
    Optional<Chamado> findWithUsuariosById(Long id);

    // --- Consultas de apoio ao dashboard -------------------------------------

    @Query("select c.status as chave, count(c) as total from Chamado c group by c.status")
    List<ContagemProjecao> contarPorStatus();

    @Query(
            "select c.status as chave, count(c) as total from Chamado c "
                    + "where c.solicitante = :solicitante group by c.status")
    List<ContagemProjecao> contarPorStatusDoSolicitante(@Param("solicitante") Usuario solicitante);

    @Query("select c.prioridade as chave, count(c) as total from Chamado c group by c.prioridade")
    List<ContagemProjecao> contarPorPrioridade();

    @Query(
            "select c.prioridade as chave, count(c) as total from Chamado c "
                    + "where c.solicitante = :solicitante group by c.prioridade")
    List<ContagemProjecao> contarPorPrioridadeDoSolicitante(@Param("solicitante") Usuario solicitante);

    @Query("select c.origemClassificacao as chave, count(c) as total from Chamado c group by c.origemClassificacao")
    List<ContagemProjecao> contarPorOrigem();

    @Query(
            "select c.origemClassificacao as chave, count(c) as total from Chamado c "
                    + "where c.solicitante = :solicitante group by c.origemClassificacao")
    List<ContagemProjecao> contarPorOrigemDoSolicitante(@Param("solicitante") Usuario solicitante);

    long countBySolicitante(Usuario solicitante);

    /**
     * Chamados abertos recentes usados na deteccao de similares.
     *
     * <p>Restringe a busca aos que ainda estao em aberto: comparar contra chamados
     * ja encerrados nao ajuda quem esta abrindo um novo.
     */
    @EntityGraph(attributePaths = {"solicitante"})
    List<Chamado> findTop50ByStatusInOrderByCriadoEmDesc(List<StatusChamado> status);

    /** Projecao de contagem agrupada usada pelo dashboard. */
    interface ContagemProjecao {
        Object getChave();

        long getTotal();
    }

    // Referencias mantidas para deixar explicito o vocabulario de filtragem.
    long countByPrioridadeAndStatusIn(Prioridade prioridade, List<StatusChamado> status);

    long countByOrigemClassificacao(OrigemClassificacao origem);
}
