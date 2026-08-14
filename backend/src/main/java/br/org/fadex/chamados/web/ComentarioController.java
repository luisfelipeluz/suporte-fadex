package br.org.fadex.chamados.web;

import br.org.fadex.chamados.exception.ApiError;
import br.org.fadex.chamados.security.UsuarioAutenticado;
import br.org.fadex.chamados.service.ComentarioService;
import br.org.fadex.chamados.web.dto.AdicionarComentarioRequest;
import br.org.fadex.chamados.web.dto.ComentarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chamados/{chamadoId}/comentarios")
@Tag(name = "Comentários", description = "Interações registradas em um chamado")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @PostMapping
    @Operation(
            summary = "Adiciona um comentário ao chamado",
            description = "Também registra a interação no histórico do chamado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comentário registrado"),
        @ApiResponse(
                responseCode = "403",
                description = "Chamado pertence a outro solicitante",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Chamado inexistente", content = @Content)
    })
    public ResponseEntity<ComentarioResponse> adicionar(
            @PathVariable Long chamadoId,
            @Valid @RequestBody AdicionarComentarioRequest requisicao,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        ComentarioResponse criado =
                comentarioService.adicionar(chamadoId, requisicao, autenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista os comentários em ordem cronológica")
    public ResponseEntity<List<ComentarioResponse>> listar(
            @PathVariable Long chamadoId,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(comentarioService.listar(chamadoId, autenticado.getUsuario()));
    }
}
