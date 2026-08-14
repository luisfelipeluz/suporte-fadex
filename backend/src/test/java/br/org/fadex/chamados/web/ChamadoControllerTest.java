package br.org.fadex.chamados.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Chamados")
class ChamadoControllerTest {

    private static final String SENHA = "suporte123";
    private static final String ADMIN = "ana.souza@fadex.org.br";
    private static final String JOAO = "joao.pereira@fadex.org.br";
    private static final String BEATRIZ = "beatriz.rocha@fadex.org.br";

    private static final String TITULO_IMPRESSORA = "Impressora do 3º andar não está funcionando";
    private static final String DESCRICAO_IMPRESSORA =
            "A impressora do 3º andar não imprime desde ontem. Aparece luz vermelha no painel "
                    + "e o setor inteiro está sem imprimir os empenhos do dia.";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String tokenAdmin;
    private String tokenJoao;
    private String tokenBeatriz;

    @BeforeEach
    void autenticarUsuarios() throws Exception {
        tokenAdmin = token(ADMIN);
        tokenJoao = token(JOAO);
        tokenBeatriz = token(BEATRIZ);
    }

    // =========================================================================
    @Nested
    @DisplayName("abertura com triagem automática")
    class Abertura {

        @Test
        @DisplayName("classifica o chamado e devolve a sugestão da IA com justificativa")
        void classificaAoAbrir() throws Exception {
            mockMvc.perform(autenticado(post("/api/chamados"), tokenJoao)
                            .content(chamado(TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("ABERTO"))
                    .andExpect(jsonPath("$.categoria").value("HARDWARE"))
                    .andExpect(jsonPath("$.prioridade").value("ALTA"))
                    .andExpect(jsonPath("$.triagem.categoriaSugerida").value("HARDWARE"))
                    .andExpect(jsonPath("$.triagem.prioridadeSugerida").value("ALTA"))
                    .andExpect(jsonPath("$.triagem.origem").value("IA"))
                    .andExpect(jsonPath("$.triagem.revisada").value(false))
                    .andExpect(jsonPath("$.triagem.divergente").value(false))
                    .andExpect(jsonPath("$.triagem.justificativa").isNotEmpty())
                    .andExpect(jsonPath("$.triagem.provedor").value("heuristic"))
                    .andExpect(jsonPath("$.solicitante.email").value(JOAO));
        }

        @Test
        @DisplayName("registra abertura e classificação no histórico")
        void registraHistorico() throws Exception {
            mockMvc.perform(autenticado(post("/api/chamados"), tokenJoao)
                            .content(chamado(TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.historico.length()").value(2))
                    .andExpect(jsonPath("$.historico[0].tipo").value("CHAMADO_ABERTO"))
                    .andExpect(jsonPath("$.historico[1].tipo").value("CLASSIFICACAO_IA"))
                    .andExpect(jsonPath("$.historico[1].etiqueta").value("CONFIANÇA ALTA"));
        }

        @Test
        @DisplayName("rejeita título e descrição ausentes com 400")
        void exigeCamposObrigatorios() throws Exception {
            mockMvc.perform(autenticado(post("/api/chamados"), tokenJoao).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.length()").value(2));
        }

        @Test
        @DisplayName("exige autenticação")
        void exigeAutenticacao() throws Exception {
            mockMvc.perform(post("/api/chamados")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(chamado(TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("listagem e filtros")
    class Listagem {

        @Test
        @DisplayName("SOLICITANTE recebe apenas os próprios chamados")
        void solicitanteVeApenasOsProprios() throws Exception {
            criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);
            criar(tokenBeatriz, "Erro no sistema financeiro", "O módulo de pagamentos retorna erro 500.");

            mockMvc.perform(autenticado(get("/api/chamados"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo[*].solicitante.email")
                            .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(JOAO))));
        }

        @Test
        @DisplayName("ADMIN enxerga chamados de todos os solicitantes")
        void adminVeTodos() throws Exception {
            criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);
            criar(tokenBeatriz, "Erro no sistema financeiro", "O módulo de pagamentos retorna erro 500.");

            mockMvc.perform(autenticado(get("/api/chamados"), tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElementos", greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("filtra por status")
        void filtraPorStatus() throws Exception {
            criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(get("/api/chamados?status=ABERTO"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo[*].status")
                            .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ABERTO"))));

            mockMvc.perform(autenticado(get("/api/chamados?status=FECHADO"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo.length()").value(0));
        }

        @Test
        @DisplayName("filtra por prioridade e por categoria")
        void filtraPorPrioridadeECategoria() throws Exception {
            criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(get("/api/chamados?prioridade=ALTA&categoria=HARDWARE"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo.length()").value(1));

            mockMvc.perform(autenticado(get("/api/chamados?categoria=REDE"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo.length()").value(0));
        }

        @Test
        @DisplayName("busca textual encontra pelo título")
        void buscaTextual() throws Exception {
            criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(get("/api/chamados?busca=impressora"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo.length()").value(1));

            mockMvc.perform(autenticado(get("/api/chamados?busca=inexistente"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo.length()").value(0));
        }

        @Test
        @DisplayName("devolve os metadados de paginação")
        void devolveMetadadosDePaginacao() throws Exception {
            criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(get("/api/chamados?page=0&size=5"), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagina").value(0))
                    .andExpect(jsonPath("$.tamanho").value(5))
                    .andExpect(jsonPath("$.primeira").value(true));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("detalhe e visibilidade")
    class Detalhe {

        @Test
        @DisplayName("SOLICITANTE recebe 403 ao acessar chamado de outro usuário")
        void chamadoDeOutroUsuarioDa403() throws Exception {
            long id = criar(tokenBeatriz, "Erro no sistema financeiro", "O módulo de pagamentos retorna erro 500.");

            mockMvc.perform(autenticado(get("/api/chamados/" + id), tokenJoao))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("ADMIN acessa o chamado de qualquer solicitante")
        void adminAcessaQualquerChamado() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(get("/api/chamados/" + id), tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
        }

        @Test
        @DisplayName("chamado inexistente devolve 404")
        void inexistenteDa404() throws Exception {
            mockMvc.perform(autenticado(get("/api/chamados/999999"), tokenAdmin))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("ciclo de vida")
    class CicloDeVida {

        @Test
        @DisplayName("ADMIN avança o status e o responsável passa a ser quem assumiu")
        void adminAvancaStatus() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/status"), tokenAdmin)
                            .content(json(Map.of("status", "EM_ANDAMENTO"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                    .andExpect(jsonPath("$.responsavel.email").value(ADMIN));
        }

        @Test
        @DisplayName("SOLICITANTE não pode alterar status (403)")
        void solicitanteNaoAlteraStatus() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/status"), tokenJoao)
                            .content(json(Map.of("status", "EM_ANDAMENTO"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("não permite pular etapas do fluxo (409)")
        void naoPermitePularEtapas() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/status"), tokenAdmin)
                            .content(json(Map.of("status", "RESOLVIDO"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem")
                            .value(org.hamcrest.Matchers.containsString("Transição de status inválida")));
        }

        @Test
        @DisplayName("chamado FECHADO não pode ser reaberto (409)")
        void chamadoFechadoNaoReabre() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            avancarStatus(id, "EM_ANDAMENTO");
            avancarStatus(id, "RESOLVIDO");
            avancarStatus(id, "FECHADO");

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/status"), tokenAdmin)
                            .content(json(Map.of("status", "EM_ANDAMENTO"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value("Chamados fechados não podem ser reabertos."));
        }

        @Test
        @DisplayName("não permite editar um chamado encerrado (409)")
        void naoEditaChamadoEncerrado() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);
            avancarStatus(id, "EM_ANDAMENTO");
            avancarStatus(id, "RESOLVIDO");
            avancarStatus(id, "FECHADO");

            mockMvc.perform(autenticado(put("/api/chamados/" + id), tokenAdmin)
                            .content(chamado("Novo título", "Nova descrição com mais de dez caracteres.")))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("ADMIN atribui responsável; SOLICITANTE recebe 403")
        void atribuicaoDeResponsavel() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);
            long idMaria = idDoUsuario("maria.lima@fadex.org.br");

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/responsavel"), tokenAdmin)
                            .content(json(Map.of("responsavelId", idMaria))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responsavel.nome").value("Maria Lima"));

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/responsavel"), tokenJoao)
                            .content(json(Map.of("responsavelId", idMaria))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("recusa responsável que não seja da equipe de suporte (409)")
        void recusaResponsavelSolicitante() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);
            long idBeatriz = idDoUsuario(BEATRIZ);

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/responsavel"), tokenAdmin)
                            .content(json(Map.of("responsavelId", idBeatriz))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("cancelamento é lógico: o chamado permanece consultável como CANCELADO")
        void cancelamentoEhLogico() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(delete("/api/chamados/" + id), tokenJoao))
                    .andExpect(status().isNoContent());

            mockMvc.perform(autenticado(get("/api/chamados/" + id), tokenJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"))
                    .andExpect(jsonPath("$.encerrado").value(true));
        }

        @Test
        @DisplayName("não permite cancelar duas vezes (409)")
        void naoCancelaDuasVezes() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(delete("/api/chamados/" + id), tokenJoao))
                    .andExpect(status().isNoContent());

            mockMvc.perform(autenticado(delete("/api/chamados/" + id), tokenJoao))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("revisão da classificação da IA")
    class RevisaoDaTriagem {

        @Test
        @DisplayName("ADMIN aceita a sugestão e a triagem passa a constar como revisada")
        void adminAceitaSugestao() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(post("/api/chamados/" + id + "/triagem/aceitar"), tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.triagem.revisada").value(true))
                    .andExpect(jsonPath("$.triagem.origem").value("IA"))
                    .andExpect(jsonPath("$.triagem.divergente").value(false));
        }

        @Test
        @DisplayName("ADMIN corrige a classificação e a sugestão original é preservada")
        void adminCorrigeClassificacao() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/triagem"), tokenAdmin)
                            .content(json(Map.of("categoria", "REDE", "prioridade", "MEDIA"))))
                    .andExpect(status().isOk())
                    // classificacao final passa a ser a do ADMIN
                    .andExpect(jsonPath("$.categoria").value("REDE"))
                    .andExpect(jsonPath("$.prioridade").value("MEDIA"))
                    .andExpect(jsonPath("$.triagem.origem").value("MANUAL"))
                    .andExpect(jsonPath("$.triagem.divergente").value(true))
                    // a sugestao original continua registrada para comparacao
                    .andExpect(jsonPath("$.triagem.categoriaSugerida").value("HARDWARE"))
                    .andExpect(jsonPath("$.triagem.prioridadeSugerida").value("ALTA"));
        }

        @Test
        @DisplayName("registra a correção no histórico")
        void registraCorrecaoNoHistorico() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/triagem"), tokenAdmin)
                            .content(json(Map.of("categoria", "REDE", "prioridade", "MEDIA"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.historico[2].tipo").value("CLASSIFICACAO_CORRIGIDA"));
        }

        @Test
        @DisplayName("SOLICITANTE não pode aceitar nem corrigir a classificação (403)")
        void solicitanteNaoRevisaClassificacao() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(post("/api/chamados/" + id + "/triagem/aceitar"), tokenJoao))
                    .andExpect(status().isForbidden());

            mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/triagem"), tokenJoao)
                            .content(json(Map.of("categoria", "REDE", "prioridade", "BAIXA"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("não permite aceitar duas vezes a mesma sugestão (409)")
        void naoAceitaDuasVezes() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(post("/api/chamados/" + id + "/triagem/aceitar"), tokenAdmin))
                    .andExpect(status().isOk());

            mockMvc.perform(autenticado(post("/api/chamados/" + id + "/triagem/aceitar"), tokenAdmin))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("comentários")
    class Comentarios {

        @Test
        @DisplayName("registra o comentário e o adiciona ao histórico")
        void registraComentario() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(post("/api/chamados/" + id + "/comentarios"), tokenJoao)
                            .content(json(Map.of("texto", "Reiniciei a impressora e o problema continua."))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.autor").value("João Pereira"))
                    .andExpect(jsonPath("$.papel").value("SOLICITANTE"))
                    .andExpect(jsonPath("$.iniciais").value("JP"));

            mockMvc.perform(autenticado(get("/api/chamados/" + id), tokenJoao))
                    .andExpect(jsonPath("$.comentarios.length()").value(1))
                    .andExpect(jsonPath("$.historico[2].tipo").value("COMENTARIO_ADICIONADO"));
        }

        @Test
        @DisplayName("devolve os comentários em ordem cronológica")
        void ordemCronologica() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            comentar(id, tokenJoao, "Primeiro comentário do solicitante.");
            comentar(id, tokenAdmin, "Segundo comentário, agora do suporte.");

            mockMvc.perform(autenticado(get("/api/chamados/" + id + "/comentarios"), tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].texto").value("Primeiro comentário do solicitante."))
                    .andExpect(jsonPath("$[1].texto").value("Segundo comentário, agora do suporte."));
        }

        @Test
        @DisplayName("não permite comentar em chamado de outro solicitante (403)")
        void naoComentaEmChamadoAlheio() throws Exception {
            long id = criar(tokenBeatriz, "Erro no sistema financeiro", "O módulo de pagamentos retorna erro 500.");

            mockMvc.perform(autenticado(post("/api/chamados/" + id + "/comentarios"), tokenJoao)
                            .content(json(Map.of("texto", "Tentando comentar em chamado alheio."))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("rejeita comentário vazio com 400")
        void rejeitaComentarioVazio() throws Exception {
            long id = criar(tokenJoao, TITULO_IMPRESSORA, DESCRICAO_IMPRESSORA);

            mockMvc.perform(autenticado(post("/api/chamados/" + id + "/comentarios"), tokenJoao)
                            .content(json(Map.of("texto", "   "))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // Apoio
    // =========================================================================

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder autenticado(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }

    private String chamado(String titulo, String descricao) throws Exception {
        return json(Map.of("titulo", titulo, "descricao", descricao));
    }

    private String token(String email) throws Exception {
        String resposta =
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("email", email, "senha", SENHA))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(resposta).get("token").asText();
    }

    private long idDoUsuario(String email) throws Exception {
        String resposta =
                mockMvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + token(email)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private long criar(String token, String titulo, String descricao) throws Exception {
        String resposta =
                mockMvc.perform(autenticado(post("/api/chamados"), token).content(chamado(titulo, descricao)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private void avancarStatus(long id, String status) throws Exception {
        mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/status"), tokenAdmin)
                        .content(json(Map.of("status", status))))
                .andExpect(status().isOk());
    }

    private void comentar(long id, String token, String texto) throws Exception {
        mockMvc.perform(autenticado(post("/api/chamados/" + id + "/comentarios"), token)
                        .content(json(Map.of("texto", texto))))
                .andExpect(status().isCreated());
    }
}
