package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacao publica de um usuario.
 *
 * <p>Nao expoe o hash da senha — nenhum endpoint da API o devolve.
 */
@Schema(name = "Usuario")
public record UsuarioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ana Souza") String nome,
        @Schema(example = "ana.souza@fadex.org.br") String email,
        @Schema(example = "ADMIN") Papel papel,
        @Schema(description = "Rótulo do papel para exibição", example = "Administrador")
        String papelRotulo,
        @Schema(description = "Iniciais usadas no avatar", example = "AS") String iniciais) {

    public static UsuarioResponse de(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getPapel().getRotulo(),
                usuario.getIniciais());
    }
}
