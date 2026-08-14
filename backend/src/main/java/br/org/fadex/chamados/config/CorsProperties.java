package br.org.fadex.chamados.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origens autorizadas a consumir a API.
 *
 * <p>Configurado por {@code CORS_ALLOWED_ORIGINS} (lista separada por virgulas)
 * em vez de liberar qualquer origem.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {}
