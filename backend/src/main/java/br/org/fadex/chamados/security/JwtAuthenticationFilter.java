package br.org.fadex.chamados.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica a requisicao a partir do token JWT.
 *
 * <p>Nao rejeita requisicoes sem token: apenas deixa o contexto de seguranca
 * vazio. Quem decide se o recurso exige autenticacao e a cadeia de filtros
 * configurada em {@code SecurityConfig}, que responde 401 quando for o caso.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CABECALHO = "Authorization";
    private static final String PREFIXO_BEARER = "Bearer ";

    /**
     * Prefixo do endpoint de eventos em tempo real.
     *
     * <p>O {@code EventSource} nativo do navegador nao permite enviar cabecalhos
     * personalizados, entao o token e aceito por query param exclusivamente nesse
     * caminho. Em qualquer outra rota apenas o cabecalho Authorization e lido.
     */
    private static final String CAMINHO_EVENTOS = "/api/eventos";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extrairToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService
                    .extrairEmail(token)
                    .ifPresent(email -> autenticar(email, request));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String email, HttpServletRequest request) {
        try {
            // O usuario e recarregado do banco a cada requisicao: o papel vigente
            // vem da base, nunca do que o cliente enviou dentro do token.
            UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);

            var autenticacao =
                    new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (UsernameNotFoundException e) {
            // Token valido de um usuario que nao existe mais: segue sem autenticar.
            logger.debug("Token válido para usuário inexistente: " + email);
        }
    }

    private String extrairToken(HttpServletRequest request) {
        String cabecalho = request.getHeader(CABECALHO);
        if (StringUtils.hasText(cabecalho) && cabecalho.startsWith(PREFIXO_BEARER)) {
            return cabecalho.substring(PREFIXO_BEARER.length()).trim();
        }

        if (request.getRequestURI().startsWith(CAMINHO_EVENTOS)) {
            String parametro = request.getParameter("token");
            if (StringUtils.hasText(parametro)) {
                return parametro.trim();
            }
        }

        return null;
    }
}
