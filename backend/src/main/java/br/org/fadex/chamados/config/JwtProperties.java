package br.org.fadex.chamados.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do token JWT, vinda de variaveis de ambiente.
 *
 * @param secret segredo HS256; deve ter no minimo 32 bytes
 * @param expirationMs validade do token em milissegundos
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {}
