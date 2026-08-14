package br.org.fadex.chamados.triage;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Confianca;
import br.org.fadex.chamados.domain.Prioridade;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Classificador deterministico baseado em lexico ponderado.
 *
 * <h2>Por que uma heuristica</h2>
 *
 * O desafio admite explicitamente heuristica propria bem justificada e afirma que
 * o criterio de avaliacao e a solucao funcionar e estar bem explicada, nao a
 * sofisticacao do modelo. Esta implementacao foi escolhida como padrao porque:
 *
 * <ul>
 *   <li><b>nao depende de rede nem de chave de API</b> — o avaliador consegue
 *       reproduzir o comportamento localmente, sem cadastro em servico externo;
 *   <li><b>e deterministica</b> — a mesma entrada produz sempre a mesma saida, o
 *       que a torna testavel de verdade (ver {@code HeuristicTriageProviderTest});
 *   <li><b>e explicavel</b> — devolve a justificativa dos termos que motivaram a
 *       decisao, e a interface exibe esse texto ao lado da sugestao;
 *   <li><b>responde em microssegundos</b> — a abertura do chamado nao fica presa
 *       esperando um servico de terceiros.
 * </ul>
 *
 * <p>Como a classificacao fica atras de {@link TriageProvider}, substitui-la por
 * um modelo real e questao de configuracao — ver {@code GeminiTriageProvider}.
 *
 * <h2>Como funciona</h2>
 *
 * <ol>
 *   <li>o texto e normalizado (minusculas, sem acentuacao);
 *   <li>cada categoria acumula pontos pelos termos de seu lexico encontrados —
 *       ocorrencias no titulo pesam mais que na descricao, porque o titulo tende a
 *       nomear o problema;
 *   <li>a prioridade vem de tres familias de sinais: bloqueio total, degradacao e
 *       rotina; termos de amplitude ("setor inteiro") elevam um nivel, pois o mesmo
 *       defeito e mais grave quando atinge muitas pessoas;
 *   <li>a confianca decorre da pontuacao e da distancia para a segunda colocada:
 *       vitoria folgada gera confianca alta, empate tecnico gera confianca baixa.
 * </ol>
 */
@Component
public class HeuristicTriageProvider implements TriageProvider {

    public static final String NOME = "heuristic";

    /** Um termo no titulo pesa mais do que o mesmo termo perdido na descricao. */
    private static final int PESO_TITULO = 3;

    private static final int PESO_DESCRICAO = 1;

    /** Lexico por categoria. Escrito com acentos e normalizado na carga da classe. */
    private static final Map<Categoria, List<Termo>> LEXICO_CATEGORIA = construirLexicoCategoria();

    /** Frase usada na justificativa de cada categoria. */
    private static final Map<Categoria, String> DOMINIO_CATEGORIA = construirDominios();

    /** Indisponibilidade ou bloqueio total do trabalho. */
    private static final List<Termo> SINAIS_BLOQUEIO =
            normalizarTodos(
                    "não imprime", "não liga", "não funciona", "não abre", "não carrega",
                    "não consigo acessar", "sem acesso", "fora do ar", "indisponível",
                    "parado", "paralisado", "urgente", "crítico", "emergência",
                    "erro 500", "bloqueado", "travou completamente", "perda de dados",
                    "não responde", "caiu", "derruba a sessão", "impede");

    /** Degradacao com contorno possivel: incomoda, mas nao impede o trabalho. */
    private static final List<Termo> SINAIS_DEGRADACAO =
            normalizarTodos(
                    "travando", "trava", "lento", "lentidão", "instável", "intermitente",
                    "divergência", "cai a cada", "desconecta", "demora", "às vezes",
                    "oscilando", "falha ocasional", "congela", "fecha sozinho");

    /** Amplitude do impacto: eleva a prioridade em um nivel. */
    private static final List<Termo> SINAIS_AMPLITUDE =
            normalizarTodos(
                    "setor inteiro", "todos os", "todas as", "ninguém consegue",
                    "várias pessoas", "toda a equipe", "pessoas afetadas",
                    "nenhuma máquina", "todo o departamento", "atendimento ao público");

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public TriageResult classificar(TriageRequest requisicao) {
        String titulo = normalizar(requisicao.titulo());
        String descricao = normalizar(requisicao.descricao());

        ResultadoCategoria categoria = classificarCategoria(titulo, descricao);
        ResultadoPrioridade prioridade = classificarPrioridade(titulo + " " + descricao);

        Confianca confianca = calcularConfianca(categoria, prioridade);

        String justificativa = montarJustificativa(categoria, prioridade);

        return new TriageResult(
                categoria.categoria(), prioridade.prioridade(), confianca, justificativa, NOME);
    }

    // =========================================================================
    // Categoria
    // =========================================================================

    private ResultadoCategoria classificarCategoria(String titulo, String descricao) {
        Map<Categoria, Integer> pontuacao = new EnumMap<>(Categoria.class);
        Map<Categoria, List<String>> termosEncontrados = new EnumMap<>(Categoria.class);

        LEXICO_CATEGORIA.forEach(
                (categoria, termos) -> {
                    int pontos = 0;
                    List<String> encontrados = new ArrayList<>();

                    for (Termo termo : termos) {
                        if (termo.ocorreEm(titulo)) {
                            pontos += PESO_TITULO;
                            encontrados.add(termo.texto());
                        } else if (termo.ocorreEm(descricao)) {
                            pontos += PESO_DESCRICAO;
                            encontrados.add(termo.texto());
                        }
                    }

                    if (pontos > 0) {
                        pontuacao.put(categoria, pontos);
                        termosEncontrados.put(categoria, encontrados);
                    }
                });

        if (pontuacao.isEmpty()) {
            return new ResultadoCategoria(Categoria.OUTROS, 0, 0, List.of());
        }

        Categoria vencedora =
                pontuacao.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(Categoria.OUTROS);

        int melhor = pontuacao.get(vencedora);
        int segunda =
                pontuacao.entrySet().stream()
                        .filter(e -> e.getKey() != vencedora)
                        .mapToInt(Map.Entry::getValue)
                        .max()
                        .orElse(0);

        return new ResultadoCategoria(
                vencedora, melhor, melhor - segunda, termosEncontrados.get(vencedora));
    }

    // =========================================================================
    // Prioridade
    // =========================================================================

    private ResultadoPrioridade classificarPrioridade(String texto) {
        List<String> bloqueio = encontrar(texto, SINAIS_BLOQUEIO);
        List<String> degradacao = encontrar(texto, SINAIS_DEGRADACAO);
        List<String> amplitude = encontrar(texto, SINAIS_AMPLITUDE);

        Prioridade prioridade;
        if (!bloqueio.isEmpty()) {
            prioridade = Prioridade.ALTA;
        } else if (!degradacao.isEmpty()) {
            prioridade = Prioridade.MEDIA;
        } else {
            prioridade = Prioridade.BAIXA;
        }

        // O mesmo defeito e mais grave quando atinge muita gente.
        if (!amplitude.isEmpty() && prioridade != Prioridade.ALTA) {
            prioridade = prioridade == Prioridade.BAIXA ? Prioridade.MEDIA : Prioridade.ALTA;
        }

        return new ResultadoPrioridade(prioridade, bloqueio, degradacao, amplitude);
    }

    // =========================================================================
    // Confianca
    // =========================================================================

    private Confianca calcularConfianca(ResultadoCategoria categoria, ResultadoPrioridade prioridade) {
        Confianca confianca;

        if (categoria.pontos() == 0) {
            confianca = Confianca.BAIXA;
        } else if (categoria.pontos() >= 4 && categoria.margem() >= 2) {
            confianca = Confianca.ALTA;
        } else if (categoria.pontos() >= 2) {
            confianca = Confianca.MEDIA;
        } else {
            confianca = Confianca.BAIXA;
        }

        // Sem nenhum sinal de urgencia, a prioridade e apenas o default: nao ha
        // como afirmar confianca alta sobre a classificacao como um todo.
        if (confianca == Confianca.ALTA && prioridade.semSinais()) {
            confianca = Confianca.MEDIA;
        }

        return confianca;
    }

    // =========================================================================
    // Justificativa
    // =========================================================================

    private String montarJustificativa(
            ResultadoCategoria categoria, ResultadoPrioridade prioridade) {

        StringBuilder texto = new StringBuilder();

        if (categoria.pontos() == 0) {
            texto.append(
                    "Nenhum padrão dominante foi identificado no texto; o chamado foi encaminhado "
                            + "para triagem geral em Outros.");
        } else {
            texto.append(DOMINIO_CATEGORIA.get(categoria.categoria()))
                    .append(" (")
                    .append(listar(categoria.termos()))
                    .append(") indicam ")
                    .append(categoria.categoria().getRotulo())
                    .append(".");
        }

        texto.append(" ");

        if (!prioridade.bloqueio().isEmpty()) {
            texto.append("Expressões de indisponibilidade ou bloqueio (")
                    .append(listar(prioridade.bloqueio()))
                    .append(") sustentam prioridade ALTA.");
        } else if (!prioridade.degradacao().isEmpty()) {
            texto.append("Sinais de degradação com contorno possível (")
                    .append(listar(prioridade.degradacao()))
                    .append(") sustentam prioridade ")
                    .append(prioridade.prioridade().getRotulo())
                    .append(".");
        } else {
            texto.append("Não há indicativo de urgência ou de bloqueio da operação, "
                    + "o que caracteriza uma solicitação de rotina.");
        }

        if (!prioridade.amplitude().isEmpty()) {
            texto.append(" O impacto relatado atinge múltiplos usuários (")
                    .append(listar(prioridade.amplitude()))
                    .append("), o que eleva a prioridade.");
        }

        return texto.toString();
    }

    // =========================================================================
    // Apoio
    // =========================================================================

    private static List<String> encontrar(String texto, List<Termo> termos) {
        return termos.stream().filter(t -> t.ocorreEm(texto)).map(Termo::texto).toList();
    }

    private static String listar(List<String> termos) {
        return String.join(", ", termos.stream().limit(3).toList());
    }

    /** Minusculas e sem acentuacao, para que "não" e "nao" sejam o mesmo termo. */
    static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String semAcento =
                Normalizer.normalize(texto, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    private static List<Termo> normalizarTodos(String... termos) {
        return List.of(termos).stream().map(Termo::de).toList();
    }

    private static Map<Categoria, List<Termo>> construirLexicoCategoria() {
        Map<Categoria, List<Termo>> lexico = new LinkedHashMap<>();

        lexico.put(
                Categoria.HARDWARE,
                normalizarTodos(
                        "impressora", "notebook", "monitor", "teclado", "mouse", "computador",
                        "equipamento", "gabinete", "cpu", "fonte", "bateria", "carregador",
                        "scanner", "headset", "webcam", "periférico", "toner", "cartucho",
                        "hd", "memória", "cabo", "tela", "não liga",
                        "impressão", "imprime", "imprimir"));

        lexico.put(
                Categoria.REDE,
                normalizarTodos(
                        "internet", "wi-fi", "wifi", "rede", "vpn", "conexão", "cabeada",
                        "roteador", "switch", "dns", "ip", "sem sinal", "servidor de arquivos",
                        "compartilhamento", "banda", "link", "ethernet"));

        lexico.put(
                Categoria.ACESSO,
                normalizarTodos(
                        "acesso", "senha", "login", "permissão", "credencial", "e-mail",
                        "email", "conta", "autenticação", "liberar", "usuário bloqueado",
                        "perfil", "drive", "pasta compartilhada", "redefinir", "caixa postal"));

        lexico.put(
                Categoria.SISTEMAS,
                normalizarTodos(
                        "sistema", "financeiro", "folha", "protocolo", "módulo", "portal",
                        "integração", "banco de dados", "relatório", "erp", "pagamento",
                        "empenho", "prestação de contas", "erro 500", "convênio"));

        lexico.put(
                Categoria.SOFTWARE,
                normalizarTodos(
                        "planilha", "programa", "instalar", "instalação", "aplicativo",
                        "editor", "word", "excel", "pdf", "office", "navegador", "licença",
                        "atualização", "versão", "software"));

        return Map.copyOf(lexico);
    }

    private static Map<Categoria, String> construirDominios() {
        Map<Categoria, String> dominios = new EnumMap<>(Categoria.class);
        dominios.put(Categoria.HARDWARE, "Termos de equipamento físico e falha de dispositivo");
        dominios.put(Categoria.REDE, "Referências a conectividade e infraestrutura de rede");
        dominios.put(Categoria.ACESSO, "Pedido de credencial, permissão ou provisionamento de acesso");
        dominios.put(Categoria.SISTEMAS, "Falha em sistema interno de gestão");
        dominios.put(Categoria.SOFTWARE, "Uso de aplicativo ou software de escritório");
        dominios.put(Categoria.OUTROS, "Sem domínio predominante");
        return Map.copyOf(dominios);
    }

    // =========================================================================
    // Estruturas internas
    // =========================================================================

    /**
     * Termo do lexico, ja normalizado e compilado.
     *
     * <p>A busca e feita por limite de palavra, e nao por substring simples: sem
     * isso "ip" casaria dentro de "equipe" e "hd" dentro de "adhesao",
     * classificando errado qualquer texto que contivesse essas palavras. O sufixo
     * opcional de plural mantem o casamento de "sistemas" com o termo "sistema".
     */
    private record Termo(String texto, Pattern padrao) {

        static Termo de(String original) {
            String normalizado = normalizar(original);
            return new Termo(
                    normalizado, Pattern.compile("\\b" + Pattern.quote(normalizado) + "(s|es)?\\b"));
        }

        boolean ocorreEm(String texto) {
            return !texto.isEmpty() && padrao.matcher(texto).find();
        }
    }

    private record ResultadoCategoria(
            Categoria categoria, int pontos, int margem, List<String> termos) {}

    private record ResultadoPrioridade(
            Prioridade prioridade,
            List<String> bloqueio,
            List<String> degradacao,
            List<String> amplitude) {

        boolean semSinais() {
            return bloqueio.isEmpty() && degradacao.isEmpty() && amplitude.isEmpty();
        }
    }
}
