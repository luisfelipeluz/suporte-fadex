package br.org.fadex.chamados.service;

import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.Comentario;
import br.org.fadex.chamados.domain.TipoEvento;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.repository.ComentarioRepository;
import br.org.fadex.chamados.web.dto.AdicionarComentarioRequest;
import br.org.fadex.chamados.web.dto.ComentarioResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Comentarios de um chamado.
 *
 * <p>A visibilidade segue exatamente a do chamado: quem nao pode ver o chamado
 * tambem nao le nem escreve seus comentarios.
 */
@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final ChamadoService chamadoService;
    private final HistoricoService historicoService;

    public ComentarioService(
            ComentarioRepository comentarioRepository,
            ChamadoService chamadoService,
            HistoricoService historicoService) {
        this.comentarioRepository = comentarioRepository;
        this.chamadoService = chamadoService;
        this.historicoService = historicoService;
    }

    @Transactional
    public ComentarioResponse adicionar(
            Long chamadoId, AdicionarComentarioRequest requisicao, Usuario autor) {

        Chamado chamado = chamadoService.buscarComPermissao(chamadoId, autor);

        Comentario comentario = new Comentario(chamado, autor, requisicao.texto().trim());
        comentarioRepository.save(comentario);

        historicoService.registrar(
                chamado,
                autor,
                TipoEvento.COMENTARIO_ADICIONADO,
                autor.getNome() + " comentou no chamado");

        return ComentarioResponse.de(comentario);
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> listar(Long chamadoId, Usuario usuario) {
        Chamado chamado = chamadoService.buscarComPermissao(chamadoId, usuario);

        return comentarioRepository.findByChamadoOrderByCriadoEmAsc(chamado).stream()
                .map(ComentarioResponse::de)
                .toList();
    }
}
