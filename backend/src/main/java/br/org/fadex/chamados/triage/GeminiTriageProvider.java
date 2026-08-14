package br.org.fadex.chamados.triage;

import br.org.fadex.chamados.config.TriageProperties;
import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Confianca;
import br.org.fadex.chamados.domain.Prioridade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Classificacao via API do Google Gemini (free tier).
 *
 * <p>Existe para demonstrar, de forma concreta, que a arquitetura de triagem nao
 * esta acoplada a nenhum fornecedor: esta classe implementa exatamente o mesmo
 * {@link TriageProvider} que a heuristica local, e alternar entre as duas e uma
 * mudanca de variavel de ambiente — nenhuma linha do dominio muda.
 *
 * <p>Fica <b>desativado por padrao</b>. Para habilitar:
 *
 * <pre>
 *   TRIAGE_PROVIDER=gemini
 *   GEMINI_API_KEY=&lt;sua chave&gt;
 * </pre>
 *
 * <p>Qualquer falha (chave ausente, timeout, cota, resposta inesperada) resulta em
 * excecao, e o {@link TriageService} cai automaticamente na heuristica local — de
 * modo que indisponibilidade do servico externo nunca impede a abertura de um
 * chamado.
 */
@Component
public class GeminiTriageProvider implements TriageProvider {

    public static final String NOME = "gemini";

    private static final Logger log = LoggerFactory.getLogger(GeminiTriageProvider.class);

    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String INSTRUCAO =
            """
            Você é o motor de triagem de uma central de chamados de TI de uma fundação de apoio \
            à pesquisa. Classifique a solicitação abaixo.

            Responda SOMENTE com um objeto JSON com exatamente estas chaves:
              "categoria": um de [HARDWARE, SOFTWARE, ACESSO, REDE, SISTEMAS, OUTROS]
              "prioridade": um de [BAIXA, MEDIA, ALTA]
              "confianca": um de [BAIXA, MEDIA, ALTA]
              "justificativa": uma frase curta, em português, explicando a decisão

            Critérios de prioridade:
              ALTA  - indisponibilidade, bloqueio do trabalho ou impacto sobre várias pessoas.
                      Também é ALTA qualquer suspeita de incidente de segurança (invasão,
                      vazamento de dados, ransomware, phishing, acesso indevido), mesmo que
                      ninguém esteja impedido de trabalhar.
              MEDIA - degradação com contorno possível
              BAIXA - solicitação de rotina, sem urgência

            Título: %s
            Descrição: %s
            """;

    private final TriageProperties.Gemini configuracao;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiTriageProvider(TriageProperties propriedades, ObjectMapper objectMapper) {
        this.configuracao = propriedades.gemini();
        this.objectMapper = objectMapper;

        var fabrica = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(1, configuracao.timeoutSeconds()));
        fabrica.setConnectTimeout(timeout);
        fabrica.setReadTimeout(timeout);

        this.restClient = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public TriageResult classificar(TriageRequest requisicao) {
        if (!configuracao.configurado()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY não configurada. Defina a chave ou use TRIAGE_PROVIDER=heuristic.");
        }

        String prompt = INSTRUCAO.formatted(requisicao.titulo(), requisicao.descricao());

        Map<String, Object> corpo =
                Map.of(
                        "contents",
                        java.util.List.of(Map.of("parts", java.util.List.of(Map.of("text", prompt)))),
                        "generationConfig",
                        Map.of("responseMimeType", "application/json", "temperature", 0.1));

        String url = URL_BASE + configuracao.model() + ":generateContent";

        String resposta =
                restClient
                        .post()
                        .uri(url)
                        // A chave vai no cabecalho, e nao na query string: URL completa
                        // costuma acabar em log de acesso, historico e mensagem de erro.
                        .header("x-goog-api-key", configuracao.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(corpo)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                (requisicaoHttp, respostaHttp) -> {
                                    throw new IllegalStateException(
                                            "Gemini respondeu "
                                                    + respostaHttp.getStatusCode()
                                                    + " para o modelo '"
                                                    + configuracao.model()
                                                    + "'.");
                                })
                        .body(String.class);

        return interpretar(resposta);
    }

    /** Extrai o JSON devolvido pelo modelo e converte para o resultado do dominio. */
    private TriageResult interpretar(String respostaBruta) {
        try {
            JsonNode raiz = objectMapper.readTree(respostaBruta);
            String conteudo =
                    raiz.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();

            if (conteudo.isBlank()) {
                throw new IllegalStateException("Resposta do Gemini sem conteúdo utilizável.");
            }

            JsonNode classificacao = objectMapper.readTree(conteudo);

            return new TriageResult(
                    lerEnum(classificacao.path("categoria").asText(), Categoria.class, Categoria.OUTROS),
                    lerEnum(classificacao.path("prioridade").asText(), Prioridade.class, Prioridade.MEDIA),
                    lerEnum(classificacao.path("confianca").asText(), Confianca.class, Confianca.MEDIA),
                    classificacao.path("justificativa").asText("Classificação gerada pelo modelo Gemini."),
                    NOME);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível interpretar a resposta do Gemini.", e);
        }
    }

    /** Converte o texto do modelo para o enum, tolerando variacoes de caixa. */
    private <E extends Enum<E>> E lerEnum(String valor, Class<E> tipo, E padrao) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Valor '{}' inválido para {}; usando {}.", valor, tipo.getSimpleName(), padrao);
            return padrao;
        }
    }
}
