package br.org.fadex.chamados.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao da triagem automatica.
 *
 * @param provider nome do {@code TriageProvider} a utilizar ({@code heuristic} ou {@code gemini})
 * @param gemini credenciais e parametros do provider baseado em API externa
 */
@ConfigurationProperties(prefix = "app.triage")
public record TriageProperties(String provider, Gemini gemini) {

    /**
     * @param apiKey chave da API; vazia desativa o provider
     * @param model identificador do modelo
     * @param timeoutSeconds limite de espera antes de cair no provider padrao
     */
    public record Gemini(String apiKey, String model, int timeoutSeconds) {

        public boolean configurado() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
