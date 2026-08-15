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

import java.time.Instant;
import java.util.Collection;
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

    // --- Candidatos a duplicado ----------------------------------------------
    //
    // A comparacao de similaridade acontece em memoria, entao a consulta precisa
    // devolver um conjunto limitado: apenas chamados recentes, ja excluidos os
    // cancelados, e no maximo o teto passado em `limite`. O mesmo par global x
    // restrito ao solicitante das contagens vale aqui — um SOLICITANTE nunca pode
    // receber, como "possivel duplicado", o titulo de um chamado alheio.

    @EntityGraph(attributePaths = {"solicitante"})
    List<Chamado> findByStatusNotInAndCriadoEmAfterOrderByCriadoEmDesc(
            Collection<StatusChamado> statusIgnorados, Instant desde, Pageable limite);

    @EntityGraph(attributePaths = {"solicitante"})
    List<Chamado> findByStatusNotInAndCriadoEmAfterAndSolicitanteOrderByCriadoEmDesc(
            Collection<StatusChamado> statusIgnorados,
            Instant desde,
            Usuario solicitante,
            Pageable limite);

    // --- Contagens do dashboard ----------------------------------------------
    //
    // Consultas derivadas, em pares: uma global (visao do ADMIN) e outra restrita
    // ao solicitante (visao do SOLICITANTE). O mesmo recorte de visibilidade da
    // listagem vale para os indicadores — o dashboard de um solicitante jamais
    // revela o volume de chamados dos demais.

    long countByStatus(StatusChamado status);

    long countByStatusAndSolicitante(StatusChamado status, Usuario solicitante);

    long countByPrioridade(Prioridade prioridade);

    long countByPrioridadeAndSolicitante(Prioridade prioridade, Usuario solicitante);

    long countByOrigemClassificacao(OrigemClassificacao origem);

    long countByOrigemClassificacaoAndSolicitante(OrigemClassificacao origem, Usuario solicitante);

    long countBySolicitante(Usuario solicitante);

    /** Chamados de uma prioridade que ainda demandam acao (nao encerrados). */
    long countByPrioridadeAndStatusNotIn(Prioridade prioridade, Collection<StatusChamado> encerrados);

    long countByPrioridadeAndStatusNotInAndSolicitante(
            Prioridade prioridade, Collection<StatusChamado> encerrados, Usuario solicitante);

    // --- Contagem por mecanismo de triagem ------------------------------------
    //
    // Agrupada na consulta, e nao enumerada em codigo como as demais: o provedor
    // e um nome livre, definido por configuracao, e nao um enum fechado. Chamados
    // anteriores ao registro do provedor ficam sob 'desconhecido' em vez de sumir
    // da soma.

    @Query(
            """
            select new br.org.fadex.chamados.repository.ContagemPorProvedor(
                       coalesce(c.provedorTriagem, 'desconhecido'), count(c))
              from Chamado c
             group by c.provedorTriagem
            """)
    List<ContagemPorProvedor> contarPorProvedorTriagem();

    @Query(
            """
            select new br.org.fadex.chamados.repository.ContagemPorProvedor(
                       coalesce(c.provedorTriagem, 'desconhecido'), count(c))
              from Chamado c
             where c.solicitante = :solicitante
             group by c.provedorTriagem
            """)
    List<ContagemPorProvedor> contarPorProvedorTriagem(@Param("solicitante") Usuario solicitante);
}
