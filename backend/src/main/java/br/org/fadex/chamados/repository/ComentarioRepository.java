package br.org.fadex.chamados.repository;

import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.Comentario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /** Ordem cronologica crescente, como exigido pelo desafio. */
    @EntityGraph(attributePaths = "autor")
    List<Comentario> findByChamadoOrderByCriadoEmAsc(Chamado chamado);

    long countByChamado(Chamado chamado);
}
