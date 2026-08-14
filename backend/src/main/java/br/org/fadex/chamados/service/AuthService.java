package br.org.fadex.chamados.service;

import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.exception.EmailJaCadastradoException;
import br.org.fadex.chamados.repository.UsuarioRepository;
import br.org.fadex.chamados.security.JwtService;
import br.org.fadex.chamados.web.dto.AutenticacaoResponse;
import br.org.fadex.chamados.web.dto.CadastroRequest;
import br.org.fadex.chamados.web.dto.LoginRequest;
import br.org.fadex.chamados.web.dto.UsuarioResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro e autenticacao de usuarios. */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Cria uma conta de solicitante.
     *
     * <p>A unicidade do e-mail e verificada aqui para produzir uma mensagem clara,
     * e garantida de fato pela constraint {@code uk_usuario_email} no banco — que
     * cobre tambem duas requisicoes simultaneas com o mesmo e-mail.
     */
    @Transactional
    public AutenticacaoResponse registrar(CadastroRequest requisicao) {
        String email = requisicao.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailJaCadastradoException(email);
        }

        Usuario usuario =
                new Usuario(
                        requisicao.nome().trim(),
                        email,
                        passwordEncoder.encode(requisicao.senha()),
                        Papel.SOLICITANTE);

        usuarioRepository.save(usuario);

        return montarResposta(usuario);
    }

    /**
     * Valida as credenciais e emite o token.
     *
     * <p>Delega ao {@link AuthenticationManager}, que compara a senha informada
     * com o hash BCrypt armazenado. Credenciais invalidas resultam em 401.
     */
    @Transactional(readOnly = true)
    public AutenticacaoResponse autenticar(LoginRequest requisicao) {
        String email = requisicao.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, requisicao.senha()));

        Usuario usuario =
                usuarioRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new IllegalStateException(
                                "Usuário autenticado não encontrado: " + email));

        return montarResposta(usuario);
    }

    private AutenticacaoResponse montarResposta(Usuario usuario) {
        return AutenticacaoResponse.de(
                jwtService.gerarToken(usuario),
                jwtService.getExpiracaoSegundos(),
                UsuarioResponse.de(usuario));
    }
}
