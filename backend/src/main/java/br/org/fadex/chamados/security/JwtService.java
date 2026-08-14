package br.org.fadex.chamados.security;

import br.org.fadex.chamados.config.JwtProperties;
import br.org.fadex.chamados.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Emissao e verificacao dos tokens JWT usados na autenticacao. */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** HS256 exige uma chave de, no minimo, 256 bits. */
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(JwtProperties propriedades) {
        byte[] bytes = propriedades.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "O segredo JWT precisa ter ao menos "
                            + TAMANHO_MINIMO_SEGREDO
                            + " bytes. Defina a variável de ambiente JWT_SECRET com um valor mais longo.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMs = propriedades.expirationMs();
    }

    /**
     * Gera um token para o usuario.
     *
     * <p>O papel vai como claim apenas para conveniencia do cliente ao montar a
     * interface; a autorizacao no servidor sempre reconsulta o usuario no banco,
     * de modo que alterar o claim no cliente nao concede privilegio algum.
     */
    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("papel", usuario.getPapel().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoMs)))
                .signWith(chave)
                .compact();
    }

    /** E-mail contido em um token valido, ou vazio se o token for invalido/expirado. */
    public Optional<String> extrairEmail(String token) {
        return extrairClaims(token).map(Claims::getSubject);
    }

    public Optional<Claims> extrairClaims(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(chave)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            // Token ausente, malformado, expirado ou com assinatura invalida.
            log.debug("Token JWT rejeitado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Validade do token em segundos, devolvida junto da resposta de login. */
    public long getExpiracaoSegundos() {
        return expiracaoMs / 1000;
    }
}
