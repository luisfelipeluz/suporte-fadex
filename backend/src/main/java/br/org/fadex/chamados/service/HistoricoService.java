package br.org.fadex.chamados.service;

import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.EventoHistorico;
import br.org.fadex.chamados.domain.TipoEvento;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.repository.EventoHistoricoRepository;
import br.org.fadex.chamados.web.dto.EventoHistoricoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro do historico de cada chamado.
 *
 * <p>Concentrar a escrita da timeline aqui evita que cada operacao de negocio
 * monte o seu proprio texto de evento e garante que nenhuma mudanca relevante
 * deixe de ser registrada.
 */
@Service
public class HistoricoService {

    private final EventoHistoricoRepository eventoRepository;

    public HistoricoService(EventoHistoricoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Transactional
    public void registrar(Chamado chamado, Usuario autor, TipoEvento tipo, String descricao) {
        eventoRepository.save(new EventoHistorico(chamado, autor, tipo, descricao));
    }

    @Transactional
    public void registrar(
            Chamado chamado, Usuario autor, TipoEvento tipo, String descricao, String etiqueta) {
        eventoRepository.save(new EventoHistorico(chamado, autor, tipo, descricao, etiqueta));
    }

    @Transactional(readOnly = true)
    public List<EventoHistoricoResponse> doChamado(Chamado chamado) {
        return eventoRepository.findByChamadoOrderByCriadoEmAscIdAsc(chamado).stream()
                .map(EventoHistoricoResponse::de)
                .toList();
    }
}
