package br.org.fadex.chamados.web.dto;

import br.org.fadex.chamados.domain.EventoHistorico;
import br.org.fadex.chamados.domain.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Evento da timeline do chamado. */
@Schema(name = "EventoHistorico")
public record EventoHistoricoResponse(
        Long id,
        TipoEvento tipo,
        String descricao,
        @Schema(description = "Rótulo curto opcional", example = "CONFIANÇA ALTA") String etiqueta,
        @Schema(description = "Cor do marcador na timeline", example = "#2563eb") String cor,
        String autor,
        Instant criadoEm) {

    public static EventoHistoricoResponse de(EventoHistorico evento) {
        return new EventoHistoricoResponse(
                evento.getId(),
                evento.getTipo(),
                evento.getDescricao(),
                evento.getEtiqueta(),
                evento.getTipo().getCor(),
                evento.getAutor() == null ? null : evento.getAutor().getNome(),
                evento.getCriadoEm());
    }
}
