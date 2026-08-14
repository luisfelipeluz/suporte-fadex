package br.org.fadex.chamados.repository;

import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Usado para listar os possiveis responsaveis por um chamado. */
    List<Usuario> findByPapelOrderByNomeAsc(Papel papel);
}
