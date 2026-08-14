package br.org.fadex.chamados.realtime;

import br.org.fadex.chamados.web.dto.ChamadoResumoResponse;

/**
 * Evento interno emitido quando um chamado e criado ou alterado.
 *
 * <p>Carrega o DTO ja montado, e nao a entidade: quando o evento e consumido, a
 * transacao ja foi confirmada e a entidade poderia estar desanexada.
 *
 * @param tipo evento SSE correspondente
 * @param chamado estado do chamado apos a alteracao
 * @param solicitanteId autor do chamado, que tambem deve ser notificado
 * @param alertaAltaPrioridade indica abertura de chamado ALTA, que dispara alerta na equipe
 */
public record ChamadoAlteradoEvento(
        TipoEventoRealtime tipo,
        ChamadoResumoResponse chamado,
        Long solicitanteId,
        boolean alertaAltaPrioridade) {

    public static ChamadoAlteradoEvento criado(ChamadoResumoResponse chamado, boolean alta) {
        return new ChamadoAlteradoEvento(
                TipoEventoRealtime.CHAMADO_CRIADO, chamado, chamado.solicitante().id(), alta);
    }

    public static ChamadoAlteradoEvento atualizado(ChamadoResumoResponse chamado) {
        return new ChamadoAlteradoEvento(
                TipoEventoRealtime.CHAMADO_ATUALIZADO, chamado, chamado.solicitante().id(), false);
    }
}
