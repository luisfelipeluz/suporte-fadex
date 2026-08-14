package br.org.fadex.chamados.service;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.domain.TipoEvento;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.exception.AcessoNegadoException;
import br.org.fadex.chamados.exception.RecursoNaoEncontradoException;
import br.org.fadex.chamados.exception.RegraDeNegocioException;
import br.org.fadex.chamados.repository.ChamadoRepository;
import br.org.fadex.chamados.repository.ChamadoSpecifications;
import br.org.fadex.chamados.repository.ComentarioRepository;
import br.org.fadex.chamados.repository.UsuarioRepository;
import br.org.fadex.chamados.triage.TriageRequest;
import br.org.fadex.chamados.triage.TriageResult;
import br.org.fadex.chamados.triage.TriageService;
import br.org.fadex.chamados.web.dto.AtualizarChamadoRequest;
import br.org.fadex.chamados.web.dto.ChamadoDetalheResponse;
import br.org.fadex.chamados.web.dto.ChamadoResumoResponse;
import br.org.fadex.chamados.web.dto.ComentarioResponse;
import br.org.fadex.chamados.web.dto.CorrigirClassificacaoRequest;
import br.org.fadex.chamados.web.dto.CriarChamadoRequest;
import br.org.fadex.chamados.web.dto.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negocio dos chamados.
 *
 * <p>Toda autorizacao relevante e aplicada aqui, e nao apenas nos controllers ou
 * na interface: o backend e a autoridade. Um SOLICITANTE que descubra o id de um
 * chamado alheio recebe 403 mesmo chamando a API diretamente.
 */
@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historicoService;
    private final TriageService triageService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            ComentarioRepository comentarioRepository,
            UsuarioRepository usuarioRepository,
            HistoricoService historicoService,
            TriageService triageService) {
        this.chamadoRepository = chamadoRepository;
        this.comentarioRepository = comentarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
        this.triageService = triageService;
    }

    // =========================================================================
    // Criacao
    // =========================================================================

    /**
     * Abre um chamado e o submete imediatamente a triagem automatica.
     *
     * <p>O solicitante nao informa categoria nem prioridade: elas vem da IA e
     * podem ser revisadas depois pelo ADMIN.
     */
    @Transactional
    public ChamadoDetalheResponse criar(CriarChamadoRequest requisicao, Usuario solicitante) {
        Chamado chamado =
                new Chamado(requisicao.titulo().trim(), requisicao.descricao().trim(), solicitante);

        TriageResult triagem =
                triageService.classificar(
                        new TriageRequest(requisicao.titulo(), requisicao.descricao()));

        chamado.aplicarTriagem(
                triagem.categoria(),
                triagem.prioridade(),
                triagem.confianca(),
                triagem.justificativa(),
                triagem.provedor());

        chamadoRepository.save(chamado);

        historicoService.registrar(
                chamado, solicitante, TipoEvento.CHAMADO_ABERTO, solicitante.getNome() + " abriu o chamado");

        historicoService.registrar(
                chamado,
                null,
                TipoEvento.CLASSIFICACAO_IA,
                "IA classificou como: "
                        + triagem.categoria().getRotulo()
                        + " / "
                        + triagem.prioridade().getRotulo(),
                "CONFIANÇA " + triagem.confianca().name());

        return montarDetalhe(chamado);
    }

    // =========================================================================
    // Consulta
    // =========================================================================

    /**
     * Lista chamados com filtros.
     *
     * <p>Para SOLICITANTE, o recorte por autoria entra como filtro na propria
     * consulta — nao ha caminho pelo qual um chamado de terceiro chegue ao
     * resultado.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResumoResponse> listar(
            Usuario usuario,
            StatusChamado status,
            Prioridade prioridade,
            Categoria categoria,
            String busca,
            Pageable paginacao) {

        Specification<Chamado> filtro =
                Specification.allOf(
                        ChamadoSpecifications.comStatus(status),
                        ChamadoSpecifications.comPrioridade(prioridade),
                        ChamadoSpecifications.comCategoria(categoria),
                        ChamadoSpecifications.comTexto(busca),
                        usuario.isAdmin() ? null : ChamadoSpecifications.doSolicitante(usuario));

        Page<Chamado> pagina = chamadoRepository.findAll(filtro, paginacao);

        return PaginaResponse.de(pagina, ChamadoResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscarDetalhe(Long id, Usuario usuario) {
        Chamado chamado = buscarComPermissao(id, usuario);
        return montarDetalhe(chamado);
    }

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Transactional
    public ChamadoDetalheResponse atualizar(
            Long id, AtualizarChamadoRequest requisicao, Usuario usuario) {

        Chamado chamado = buscarComPermissao(id, usuario);
        chamado.atualizarConteudo(requisicao.titulo().trim(), requisicao.descricao().trim());

        historicoService.registrar(
                chamado,
                usuario,
                TipoEvento.CHAMADO_ATUALIZADO,
                usuario.getNome() + " editou o conteúdo do chamado");

        return montarDetalhe(chamado);
    }

    /** Avanco de status. Exclusivo do ADMIN. */
    @Transactional
    public ChamadoDetalheResponse alterarStatus(Long id, StatusChamado novoStatus, Usuario admin) {
        Chamado chamado = buscar(id);

        chamado.alterarStatus(novoStatus);

        // Ao entrar em andamento sem responsavel definido, quem move assume.
        if (novoStatus == StatusChamado.EM_ANDAMENTO && chamado.getResponsavel() == null) {
            chamado.atribuirResponsavel(admin);
            historicoService.registrar(
                    chamado,
                    admin,
                    TipoEvento.RESPONSAVEL_ATRIBUIDO,
                    admin.getNome() + " assumiu o chamado");
        }

        historicoService.registrar(
                chamado,
                admin,
                TipoEvento.STATUS_ALTERADO,
                "Status alterado para: " + novoStatus.getRotulo());

        return montarDetalhe(chamado);
    }

    /** Atribuicao ou reatribuicao de responsavel. Exclusiva do ADMIN. */
    @Transactional
    public ChamadoDetalheResponse atribuirResponsavel(Long id, Long responsavelId, Usuario admin) {
        Chamado chamado = buscar(id);

        Usuario responsavel =
                usuarioRepository
                        .findById(responsavelId)
                        .orElseThrow(() -> RecursoNaoEncontradoException.usuario(responsavelId));

        if (responsavel.getPapel() != Papel.ADMIN) {
            throw new RegraDeNegocioException(
                    "Apenas usuários da equipe de suporte podem ser responsáveis por um chamado.");
        }

        chamado.atribuirResponsavel(responsavel);

        historicoService.registrar(
                chamado,
                admin,
                TipoEvento.RESPONSAVEL_ATRIBUIDO,
                responsavel.getNome() + " assumiu o chamado");

        return montarDetalhe(chamado);
    }

    /**
     * Cancelamento logico do chamado.
     *
     * <p>O registro e preservado com status CANCELADO em vez de removido, para que
     * o historico da operacao nao desapareca do sistema.
     */
    @Transactional
    public void cancelar(Long id, Usuario usuario) {
        Chamado chamado = buscarComPermissao(id, usuario);

        chamado.cancelar();

        historicoService.registrar(
                chamado,
                usuario,
                TipoEvento.CHAMADO_CANCELADO,
                "Chamado cancelado por " + usuario.getNome());
    }

    // =========================================================================
    // Revisao da classificacao pela IA
    // =========================================================================

    /** ADMIN confirma a sugestao da IA. */
    @Transactional
    public ChamadoDetalheResponse aceitarClassificacao(Long id, Usuario admin) {
        Chamado chamado = buscar(id);

        chamado.aceitarClassificacaoIa();

        historicoService.registrar(
                chamado,
                admin,
                TipoEvento.CLASSIFICACAO_ACEITA,
                "Classificação da IA aceita por " + admin.getNome());

        return montarDetalhe(chamado);
    }

    /** ADMIN corrige a classificacao; a sugestao original permanece registrada. */
    @Transactional
    public ChamadoDetalheResponse corrigirClassificacao(
            Long id, CorrigirClassificacaoRequest requisicao, Usuario admin) {

        Chamado chamado = buscar(id);

        chamado.corrigirClassificacao(requisicao.categoria(), requisicao.prioridade());

        historicoService.registrar(
                chamado,
                admin,
                TipoEvento.CLASSIFICACAO_CORRIGIDA,
                "Classificação corrigida para: "
                        + requisicao.categoria().getRotulo()
                        + " / "
                        + requisicao.prioridade().getRotulo()
                        + " por "
                        + admin.getNome());

        return montarDetalhe(chamado);
    }

    // =========================================================================
    // Apoio
    // =========================================================================

    /** Busca sem verificacao de autoria; usar apenas em operacoes ja restritas ao ADMIN. */
    Chamado buscar(Long id) {
        return chamadoRepository
                .findWithUsuariosById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.chamado(id));
    }

    /**
     * Busca aplicando o recorte de visibilidade.
     *
     * <p>Devolve 404 para chamado inexistente e 403 para chamado de outro usuario.
     */
    Chamado buscarComPermissao(Long id, Usuario usuario) {
        Chamado chamado = buscar(id);

        if (!usuario.isAdmin() && !chamado.pertenceA(usuario)) {
            throw AcessoNegadoException.chamadoDeOutroUsuario();
        }

        return chamado;
    }

    ChamadoDetalheResponse montarDetalhe(Chamado chamado) {
        List<ComentarioResponse> comentarios =
                comentarioRepository.findByChamadoOrderByCriadoEmAsc(chamado).stream()
                        .map(ComentarioResponse::de)
                        .toList();

        return ChamadoDetalheResponse.de(
                chamado, historicoService.doChamado(chamado), comentarios);
    }
}
