package br.org.fadex.chamados.repository;

import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.EventoHistorico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, Long> {

    /** Timeline do chamado, do evento mais antigo para o mais recente. */
    @EntityGraph(attributePaths = "autor")
    List<EventoHistorico> findByChamadoOrderByCriadoEmAscIdAsc(Chamado chamado);
}
