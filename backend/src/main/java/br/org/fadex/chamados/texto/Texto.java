package br.org.fadex.chamados.texto;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Preparo de texto livre para comparacao.
 *
 * <p>Dois mecanismos do sistema leem a mesma linguagem escrita pelo solicitante: a
 * triagem automatica, que procura termos de um lexico, e a deteccao de duplicados,
 * que compara chamados entre si. Ambos precisam enxergar "Não Imprime" e
 * "nao imprime" como a mesma coisa, e por isso a normalizacao mora aqui, em um
 * unico lugar, em vez de ser reescrita em cada um deles.
 */
public final class Texto {

    /** Qualquer sequencia que nao seja letra ou digito separa tokens. */
    private static final Pattern SEPARADOR = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    /** Abaixo disto o token quase sempre e ruido ("de", "ja", "os"). */
    private static final int TAMANHO_MINIMO = 3;

    /**
     * Palavras sem poder discriminante neste dominio.
     *
     * <p>Alem das preposicoes e conjugacoes comuns do portugues, inclui a cortesia
     * e o jargao que aparecem em praticamente todo chamado ("bom dia", "solicito",
     * "favor"). Mantidas, elas aproximariam chamados que nao tem nada em comum
     * exceto a forma de escrever um pedido.
     */
    private static final Set<String> STOPWORDS =
            Set.of(
                    // gramatica
                    "que", "com", "sem", "por", "para", "pelo", "pela", "pelos", "pelas",
                    "dos", "das", "nos", "nas", "num", "numa", "uma", "uns", "umas", "aos",
                    "seu", "sua", "seus", "suas", "meu", "minha", "meus", "minhas",
                    "nosso", "nossa", "este", "esta", "esse", "essa", "isso", "isto",
                    "aquele", "aquela", "aqui", "ali", "onde", "quando", "qual", "quais",
                    "como", "mais", "menos", "muito", "muita", "pouco", "todo", "toda",
                    "todos", "todas", "sobre", "entre", "apos", "ate", "desde", "ainda",
                    "tambem", "porem", "mas", "nao", "sim", "sao", "foi", "ser", "sendo",
                    "estao", "estou", "tem", "tenho", "temos", "ter", "havia", "houve",
                    "fica", "ficou", "vai", "vou", "faz", "fazer", "novamente", "sempre",
                    "nunca",
                    // cortesia e jargao de abertura de chamado
                    "favor", "gentileza", "obrigado", "obrigada", "prezados", "prezada",
                    "bom", "boa", "dia", "tarde", "noite", "solicito", "solicitacao",
                    "solicitar", "chamado", "abertura", "abrir", "preciso", "precisa",
                    "gostaria", "poderia", "podem", "pode", "equipe", "suporte", "setor");

    private Texto() {
        // utilitario
    }

    /** Minusculas e sem acentuacao, para que "não" e "nao" sejam o mesmo termo. */
    public static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String semAcento =
                Normalizer.normalize(texto, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    /**
     * Quebra o texto em termos significativos, ja normalizados.
     *
     * <p>Descarta stopwords e tokens curtos demais. A ordem de aparicao e
     * preservada e as repeticoes sao mantidas: quem conta frequencia decide o que
     * fazer com elas.
     */
    public static List<String> tokenizar(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String bruto : SEPARADOR.split(normalizado)) {
            if (bruto.length() >= TAMANHO_MINIMO && !STOPWORDS.contains(bruto)) {
                tokens.add(bruto);
            }
        }
        return tokens;
    }
}
