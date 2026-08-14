package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.Comentario;
import br.org.fadex.chamados.domain.Papel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "Comentario")
public record ComentarioResponse(
        Long id,
        String autor,
        @Schema(description = "Iniciais do autor para o avatar", example = "ML") String iniciais,
        Papel papel,
        String texto,
        Instant criadoEm) {

    public static ComentarioResponse de(Comentario comentario) {
        return new ComentarioResponse(
                comentario.getId(),
                comentario.getAutor().getNome(),
                comentario.getAutor().getIniciais(),
                comentario.getAutor().getPapel(),
                comentario.getTexto(),
                comentario.getCriadoEm());
    }
}
