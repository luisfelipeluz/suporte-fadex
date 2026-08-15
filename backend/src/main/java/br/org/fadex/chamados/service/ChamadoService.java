package br.org.fadex.chamados.service;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.domain.TipoEvento;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.duplicados.DetectorDuplicados;
import br.org.fadex.chamados.exception.AcessoNegadoException;
import br.org.fadex.chamados.exception.RecursoNaoEncontradoException;
import br.org.fadex.chamados.exception.RegraDeNegocioException;
import br.org.fadex.chamados.realtime.ChamadoAlteradoEvento;
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
import br.org.fadex.chamados.web.dto.ChamadoSimilarResponse;
import br.org.fadex.chamados.web.dto.ComentarioResponse;
import br.org.fadex.chamados.web.dto.CorrigirClassificacaoRequest;
import br.org.fadex.chamados.web.dto.CriarChamadoRequest;
import br.org.fadex.chamados.web.dto.PaginaResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

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
    private final DetectorDuplicados detectorDuplicados;
    private final ApplicationEventPublisher eventPublisher;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            ComentarioRepository comentarioRepository,
            UsuarioRepository usuarioRepository,
            HistoricoService historicoService,
            TriageService triageService,
            DetectorDuplicados detectorDuplicados,
            ApplicationEventPublisher eventPublisher) {
        this.chamadoRepository = chamadoRepository;
        this.comentarioRepository = comentarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
        this.triageService = triageService;
        this.detectorDuplicados = detectorDuplicados;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Anuncia a alteracao para o mecanismo de tempo real.
     *
     * <p>Publica um evento de aplicacao em vez de chamar o SSE diretamente: quem
     * escuta so age depois do commit, evitando notificar o painel sobre uma
     * mudanca que ainda pode sofrer rollback.
     */
    private void notificar(Chamado chamado, boolean criacao) {
        ChamadoResumoResponse resumo = ChamadoResumoResponse.de(chamado);

        eventPublisher.publishEvent(
                criacao
                        ? ChamadoAlteradoEvento.criado(
                                resumo, chamado.getPrioridade().exigeAlertaImediato())
                        : ChamadoAlteradoEvento.atualizado(resumo));
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
                // Rotulo acentuado ("MÉDIA"), e nao o nome ASCII da constante.
                "CONFIANÇA " + triagem.confianca().getRotulo().toUpperCase(Locale.ROOT));

        notificar(chamado, true);

        return montarDetalhe(chamado, solicitante);
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
        return montarDetalhe(chamado, usuario);
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

        notificar(chamado, false);

        return montarDetalhe(chamado, usuario);
    }

    /**
     * Movimentacao de status: avanco, retorno ou encerramento direto.
     *
     * <p>A autorizacao aqui e por transicao, e nao por papel puro. O ADMIN conduz o
     * fluxo inteiro; o SOLICITANTE tem exatamente duas acoes, e so no proprio
     * chamado resolvido — confirmar a resolucao ou dizer que o problema continua.
     * E o que da sentido ao estado RESOLVIDO: ele existe justamente para aguardar
     * essa resposta antes do encerramento definitivo.
     *
     * <p>O historico distingue cada caso e nomeia quem agiu: um retorno de RESOLVIDO
     * para EM_ANDAMENTO significa que o atendimento nao resolveu o problema, e quem
     * auditar depois precisa ver de onde o chamado voltou e por ordem de quem.
     */
    @Transactional
    public ChamadoDetalheResponse alterarStatus(Long id, StatusChamado novoStatus, Usuario autor) {
        Chamado chamado = buscar(id);

        StatusChamado origem = chamado.getStatus();
        verificarPermissaoDeStatus(chamado, origem, novoStatus, autor);

        boolean retorno = origem.isRetrocessoPara(novoStatus);

        chamado.alterarStatus(novoStatus);

        // Ao entrar em andamento sem responsavel definido, quem move assume. So
        // vale para o ADMIN: o solicitante que reabre o atendimento nao pode virar
        // responsavel por ele — responsavel e sempre alguem da equipe de suporte.
        if (novoStatus == StatusChamado.EM_ANDAMENTO
                && chamado.getResponsavel() == null
                && autor.isAdmin()) {
            chamado.atribuirResponsavel(autor);
            historicoService.registrar(
                    chamado,
                    autor,
                    TipoEvento.RESPONSAVEL_ATRIBUIDO,
                    autor.getNome() + " assumiu o chamado");
        }

        registrarMudancaDeStatus(chamado, origem, novoStatus, autor, retorno);

        notificar(chamado, false);

        return montarDetalhe(chamado, autor);
    }

    /**
     * Aplica o recorte de quem pode fazer cada transicao.
     *
     * <p>Roda antes de {@code chamado.alterarStatus}, para que uma tentativa sem
     * permissao devolva 403 em vez de 409: o solicitante precisa saber que a
     * operacao nao e dele, e nao que o fluxo a proibe para todos.
     */
    private void verificarPermissaoDeStatus(
            Chamado chamado, StatusChamado origem, StatusChamado destino, Usuario autor) {

        if (autor.isAdmin()) {
            return;
        }

        boolean proprioChamado = chamado.pertenceA(autor);
        boolean aguardandoValidacao = origem == StatusChamado.RESOLVIDO;
        boolean confirmaOuContesta =
                destino == StatusChamado.FECHADO || destino == StatusChamado.EM_ANDAMENTO;

        if (!proprioChamado || !aguardandoValidacao || !confirmaOuContesta) {
            throw AcessoNegadoException.mudancaDeStatusRestrita();
        }
    }

    /** Texto do historico, que muda conforme quem agiu e para onde o chamado foi. */
    private void registrarMudancaDeStatus(
            Chamado chamado,
            StatusChamado origem,
            StatusChamado destino,
            Usuario autor,
            boolean retorno) {

        if (retorno) {
            boolean contestacao = !autor.isAdmin();
            historicoService.registrar(
                    chamado,
                    autor,
                    TipoEvento.STATUS_RETROCEDIDO,
                    contestacao
                            ? autor.getNome()
                                    + " informou que o problema continua; o atendimento foi reaberto"
                            : autor.getNome()
                                    + " retornou o chamado de "
                                    + origem.getRotulo()
                                    + " para "
                                    + destino.getRotulo(),
                    "RETORNO");
            return;
        }

        // O solicitante fechando o proprio chamado resolvido esta confirmando a
        // solucao — registrar isso como "moveu para Fechado" perderia o sentido.
        if (!autor.isAdmin() && destino == StatusChamado.FECHADO) {
            historicoService.registrar(
                    chamado,
                    autor,
                    TipoEvento.STATUS_ALTERADO,
                    autor.getNome() + " confirmou a resolução e encerrou o chamado",
                    "CONFIRMADO");
            return;
        }

        boolean encerramentoDireto =
                destino == StatusChamado.FECHADO
                        && origem.proximo().filter(StatusChamado.FECHADO::equals).isEmpty();

        historicoService.registrar(
                chamado,
                autor,
                TipoEvento.STATUS_ALTERADO,
                encerramentoDireto
                        ? autor.getNome()
                                + " encerrou o chamado direto de "
                                + origem.getRotulo()
                        : autor.getNome() + " moveu o chamado para " + destino.getRotulo(),
                encerramentoDireto ? "ENCERRAMENTO DIRETO" : null);
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

        notificar(chamado, false);

        return montarDetalhe(chamado, admin);
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

        notificar(chamado, false);
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

        notificar(chamado, false);

        return montarDetalhe(chamado, admin);
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

        notificar(chamado, false);

        return montarDetalhe(chamado, admin);
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

    /**
     * Monta o detalhe do chamado ja com os possiveis duplicados.
     *
     * <p>O {@code observador} nao e decorativo: e ele que define quais chamados
     * entram na comparacao de similaridade. Passar o usuario da requisicao, e nao
     * o solicitante do chamado, e o que impede que a lista de duplicados revele
     * chamados que esse usuario nao teria permissao de ler.
     */
    ChamadoDetalheResponse montarDetalhe(Chamado chamado, Usuario observador) {
        List<ComentarioResponse> comentarios =
                comentarioRepository.findByChamadoOrderByCriadoEmAsc(chamado).stream()
                        .map(ComentarioResponse::de)
                        .toList();

        return ChamadoDetalheResponse.de(
                chamado,
                historicoService.doChamado(chamado),
                comentarios,
                similares(chamado, observador));
    }

    /** Possiveis duplicados do chamado, na visao do usuario informado. */
    @Transactional(readOnly = true)
    public List<ChamadoSimilarResponse> similares(Long id, Usuario usuario) {
        return similares(buscarComPermissao(id, usuario), usuario);
    }

    private List<ChamadoSimilarResponse> similares(Chamado chamado, Usuario observador) {
        return detectorDuplicados.similaresA(chamado, observador).stream()
                .map(ChamadoSimilarResponse::de)
                .toList();
    }
}
