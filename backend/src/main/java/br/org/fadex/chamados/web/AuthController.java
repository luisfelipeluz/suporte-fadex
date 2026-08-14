package br.org.fadex.chamados.web;

import br.org.fadex.chamados.exception.ApiError;
import br.org.fadex.chamados.security.UsuarioAutenticado;
import br.org.fadex.chamados.service.AuthService;
import br.org.fadex.chamados.web.dto.AutenticacaoResponse;
import br.org.fadex.chamados.web.dto.CadastroRequest;
import br.org.fadex.chamados.web.dto.LoginRequest;
import br.org.fadex.chamados.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e sessão do usuário")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    @SecurityRequirements // endpoint publico: nao exige token
    @Operation(
            summary = "Cadastra um novo solicitante",
            description =
                    """
                    Cria uma conta com papel SOLICITANTE e já devolve um token válido.

                    O papel não é aceito no corpo da requisição: contas ADMIN são criadas
                    pela migration de seed, não pelo cadastro público.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado"),
        @ApiResponse(
                responseCode = "400",
                description = "Campos obrigatórios ausentes ou e-mail inválido",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "E-mail já cadastrado",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AutenticacaoResponse> registrar(
            @Valid @RequestBody CadastroRequest requisicao) {

        AutenticacaoResponse resposta = authService.registrar(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/login")
    @SecurityRequirements // endpoint publico: nao exige token
    @Operation(
            summary = "Autentica e emite um token JWT",
            description =
                    """
                    Credenciais de teste criadas pelo seed:

                    | Papel | E-mail | Senha |
                    |---|---|---|
                    | ADMIN | `ana.souza@fadex.org.br` | `suporte123` |
                    | SOLICITANTE | `joao.pereira@fadex.org.br` | `suporte123` |
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticado"),
        @ApiResponse(
                responseCode = "401",
                description = "Credenciais inválidas",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AutenticacaoResponse> login(@Valid @RequestBody LoginRequest requisicao) {
        return ResponseEntity.ok(authService.autenticar(requisicao));
    }

    @GetMapping("/eu")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Dados do usuário autenticado",
            description = "Permite ao frontend restaurar a sessão a partir de um token salvo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário da sessão atual"),
        @ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UsuarioResponse> eu(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(UsuarioResponse.de(autenticado.getUsuario()));
    }
}
