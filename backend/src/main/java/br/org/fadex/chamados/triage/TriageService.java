package br.org.fadex.chamados.triage;

import br.org.fadex.chamados.config.TriageProperties;
import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Confianca;
import br.org.fadex.chamados.domain.Prioridade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestra a triagem automatica.
 *
 * <p>Esta classe e a fronteira entre o dominio e os mecanismos de classificacao: o
 * {@code ChamadoService} pede uma triagem e recebe um {@link TriageResult}, sem
 * jamais saber qual provider respondeu.
 *
 * <p>Duas garantias importantes:
 *
 * <ul>
 *   <li><b>selecao por configuracao</b> — o provider vem de {@code TRIAGE_PROVIDER};
 *       um nome desconhecido cai no padrao em vez de derrubar a aplicacao;
 *   <li><b>degradacao graciosa</b> — se o provider escolhido falhar (rede fora,
 *       cota estourada, timeout), a triagem cai automaticamente na heuristica
 *       local. A abertura de um chamado nunca falha por causa da IA.
 * </ul>
 */
@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final Map<String, TriageProvider> providersPorNome;
    private final TriageProvider providerPadrao;
    private final String nomeConfigurado;

    public TriageService(
            List<TriageProvider> providers,
            HeuristicTriageProvider heuristico,
            TriageProperties propriedades) {

        this.providersPorNome =
                providers.stream()
                        .collect(Collectors.toMap(TriageProvider::nome, Function.identity()));
        this.providerPadrao = heuristico;
        this.nomeConfigurado =
                propriedades.provider() == null || propriedades.provider().isBlank()
                        ? HeuristicTriageProvider.NOME
                        : propriedades.provider().trim().toLowerCase();

        if (!providersPorNome.containsKey(nomeConfigurado)) {
            log.warn(
                    "Provider de triagem '{}' não encontrado. Providers disponíveis: {}. "
                            + "Usando '{}' como padrão.",
                    nomeConfigurado,
                    providersPorNome.keySet(),
                    providerPadrao.nome());
        } else {
            log.info("Triagem automática usando o provider '{}'.", nomeConfigurado);
        }
    }

    /**
     * Classifica o chamado, sempre devolvendo um resultado utilizavel.
     *
     * <p>Nunca propaga excecao: a triagem e um enriquecimento da abertura do
     * chamado, e nao uma condicao para que ela ocorra.
     */
    public TriageResult classificar(TriageRequest requisicao) {
        TriageProvider provider = providersPorNome.getOrDefault(nomeConfigurado, providerPadrao);

        try {
            return provider.classificar(requisicao);
        } catch (RuntimeException e) {
            log.warn(
                    "Provider de triagem '{}' falhou ({}). Aplicando a heurística local.",
                    provider.nome(),
                    e.getMessage());
            return classificarComPadrao(requisicao, provider);
        }
    }

    private TriageResult classificarComPadrao(TriageRequest requisicao, TriageProvider queFalhou) {
        if (queFalhou == providerPadrao) {
            // A heuristica local ja era o padrao e ainda assim falhou: devolve uma
            // classificacao neutra para que o chamado consiga ser aberto.
            log.error("A heurística local falhou. Aplicando classificação neutra.");
            return classificacaoNeutra();
        }
        try {
            return providerPadrao.classificar(requisicao);
        } catch (RuntimeException e) {
            log.error("Falha também na heurística local.", e);
            return classificacaoNeutra();
        }
    }

    private TriageResult classificacaoNeutra() {
        return new TriageResult(
                Categoria.OUTROS,
                Prioridade.MEDIA,
                Confianca.BAIXA,
                "A triagem automática não pôde ser concluída. O chamado foi encaminhado "
                        + "para classificação manual pela equipe de suporte.",
                "indisponivel");
    }

    /** Nome do provider em uso, exposto na documentacao e no detalhe do chamado. */
    public String getProviderEmUso() {
        return providersPorNome.containsKey(nomeConfigurado)
                ? nomeConfigurado
                : providerPadrao.nome();
    }
}
