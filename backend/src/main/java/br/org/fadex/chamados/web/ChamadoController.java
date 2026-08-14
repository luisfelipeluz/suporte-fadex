package br.org.fadex.chamados.web;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.exception.ApiError;
import br.org.fadex.chamados.security.UsuarioAutenticado;
import br.org.fadex.chamados.service.ChamadoService;
import br.org.fadex.chamados.web.dto.AlterarStatusRequest;
import br.org.fadex.chamados.web.dto.AtribuirResponsavelRequest;
import br.org.fadex.chamados.web.dto.AtualizarChamadoRequest;
import br.org.fadex.chamados.web.dto.ChamadoDetalheResponse;
import br.org.fadex.chamados.web.dto.ChamadoResumoResponse;
import br.org.fadex.chamados.web.dto.CorrigirClassificacaoRequest;
import br.org.fadex.chamados.web.dto.CriarChamadoRequest;
import br.org.fadex.chamados.web.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/chamados")
@Tag(name = "Chamados", description = "Abertura, consulta, ciclo de vida e revisão da triagem")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    // =========================================================================
    // CRUD
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Abre um chamado (com triagem automática)",
            description =
                    """
                    O solicitante informa apenas título e descrição. O sistema classifica o
                    texto automaticamente e devolve a sugestão de categoria e prioridade no
                    bloco `triagem`, junto da justificativa da decisão.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chamado aberto e classificado"),
        @ApiResponse(
                responseCode = "400",
                description = "Título ou descrição ausentes",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    public ResponseEntity<ChamadoDetalheResponse> criar(
            @Valid @RequestBody CriarChamadoRequest requisicao,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        ChamadoDetalheResponse criado = chamadoService.criar(requisicao, autenticado.getUsuario());

        return ResponseEntity.created(URI.create("/api/chamados/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(
            summary = "Lista chamados com filtros e paginação",
            description =
                    """
                    ADMIN enxerga todos os chamados; SOLICITANTE recebe apenas os próprios —
                    o recorte é aplicado na consulta, não na interface.
                    """)
    @ApiResponse(responseCode = "200", description = "Página de chamados")
    public ResponseEntity<PaginaResponse<ChamadoResumoResponse>> listar(
            @Parameter(description = "Filtra por status") @RequestParam(required = false)
                    StatusChamado status,
            @Parameter(description = "Filtra por prioridade") @RequestParam(required = false)
                    Prioridade prioridade,
            @Parameter(description = "Filtra por categoria") @RequestParam(required = false)
                    Categoria categoria,
            @Parameter(description = "Busca por título, descrição ou número")
                    @RequestParam(required = false)
                    String busca,
            @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC)
                    Pageable paginacao,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(
                chamadoService.listar(
                        autenticado.getUsuario(), status, prioridade, categoria, busca, paginacao));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um chamado com triagem, histórico e comentários")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chamado encontrado"),
        @ApiResponse(
                responseCode = "403",
                description = "Chamado pertence a outro solicitante",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Chamado inexistente",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ChamadoDetalheResponse> detalhar(
            @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(chamadoService.buscarDetalhe(id, autenticado.getUsuario()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita título e descrição de um chamado ainda em aberto")
    @ApiResponse(responseCode = "409", description = "Chamado encerrado", content = @Content)
    public ResponseEntity<ChamadoDetalheResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarChamadoRequest requisicao,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(
                chamadoService.atualizar(id, requisicao, autenticado.getUsuario()));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancela um chamado",
            description =
                    """
                    Cancelamento lógico: o chamado passa a `CANCELADO` e permanece consultável
                    com todo o seu histórico, em vez de ser removido do banco.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Chamado cancelado"),
        @ApiResponse(responseCode = "409", description = "Chamado já encerrado", content = @Content)
    })
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        chamadoService.cancelar(id, autenticado.getUsuario());
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Ciclo de vida — exclusivo do ADMIN
    // =========================================================================

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Altera o status do chamado (ADMIN)",
            description =
                    """
                    O fluxo é sequencial: ABERTO → EM_ANDAMENTO → RESOLVIDO → FECHADO.
                    Chamados FECHADOS não podem ser reabertos — a tentativa devolve 409.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status alterado"),
        @ApiResponse(
                responseCode = "403",
                description = "Operação exclusiva do ADMIN",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Transição inválida ou chamado encerrado",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ChamadoDetalheResponse> alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AlterarStatusRequest requisicao,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(
                chamadoService.alterarStatus(id, requisicao.status(), autenticado.getUsuario()));
    }

    @PatchMapping("/{id}/responsavel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atribui ou reatribui o responsável (ADMIN)")
    @ApiResponse(responseCode = "403", description = "Operação exclusiva do ADMIN", content = @Content)
    public ResponseEntity<ChamadoDetalheResponse> atribuirResponsavel(
            @PathVariable Long id,
            @Valid @RequestBody AtribuirResponsavelRequest requisicao,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(
                chamadoService.atribuirResponsavel(
                        id, requisicao.responsavelId(), autenticado.getUsuario()));
    }

    // =========================================================================
    // Revisão da triagem — exclusiva do ADMIN
    // =========================================================================

    @PostMapping("/{id}/triagem/aceitar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Aceita a sugestão da IA (ADMIN)",
            description = "Confirma a classificação sugerida e marca a triagem como revisada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sugestão aceita"),
        @ApiResponse(responseCode = "403", description = "Exclusivo do ADMIN", content = @Content),
        @ApiResponse(responseCode = "409", description = "Classificação já aceita", content = @Content)
    })
    public ResponseEntity<ChamadoDetalheResponse> aceitarTriagem(
            @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(chamadoService.aceitarClassificacao(id, autenticado.getUsuario()));
    }

    @PatchMapping("/{id}/triagem")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Corrige a classificação da IA (ADMIN)",
            description =
                    """
                    Substitui a categoria e a prioridade vigentes e passa a origem para MANUAL.
                    A sugestão original da IA é preservada para comparação e auditoria.
                    """)
    @ApiResponse(responseCode = "403", description = "Exclusivo do ADMIN", content = @Content)
    public ResponseEntity<ChamadoDetalheResponse> corrigirTriagem(
            @PathVariable Long id,
            @Valid @RequestBody CorrigirClassificacaoRequest requisicao,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(
                chamadoService.corrigirClassificacao(id, requisicao, autenticado.getUsuario()));
    }
}
