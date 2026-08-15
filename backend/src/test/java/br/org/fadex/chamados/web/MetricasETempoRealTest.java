package br.org.fadex.chamados.web;

import br.org.fadex.chamados.realtime.ChamadoAlteradoEvento;
import br.org.fadex.chamados.realtime.TipoEventoRealtime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Indicadores e publicacao de eventos em tempo real.
 *
 * <p>Note que estes testes <b>nao</b> usam {@code @Transactional}: os eventos sao
 * publicados na fase AFTER_COMMIT, e em um teste transacional o commit nunca
 * acontece — nenhum evento seria emitido. Por isso a limpeza e feita a mao.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Indicadores e tempo real")
class MetricasETempoRealTest {

    private static final String SENHA = "suporte123";
    private static final String ADMIN = "ana.souza@fadex.org.br";
    private static final String JOAO = "joao.pereira@fadex.org.br";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ColetorDeEventos coletor;
    @Autowired private br.org.fadex.chamados.repository.EventoHistoricoRepository eventoRepository;
    @Autowired private br.org.fadex.chamados.repository.ComentarioRepository comentarioRepository;
    @Autowired private br.org.fadex.chamados.repository.ChamadoRepository chamadoRepository;

    private String tokenAdmin;
    private String tokenJoao;

    /** Componente de teste que captura os eventos publicados apos o commit. */
    @Component
    static class ColetorDeEventos {
        final List<ChamadoAlteradoEvento> eventos = new CopyOnWriteArrayList<>();

        @EventListener
        void capturar(ChamadoAlteradoEvento evento) {
            eventos.add(evento);
        }

        void limpar() {
            eventos.clear();
        }
    }

    @BeforeEach
    void preparar() throws Exception {
        // Ordem importa: eventos e comentarios referenciam chamados.
        eventoRepository.deleteAllInBatch();
        comentarioRepository.deleteAllInBatch();
        chamadoRepository.deleteAllInBatch();

        coletor.limpar();

        tokenAdmin = token(ADMIN);
        tokenJoao = token(JOAO);
    }

    // =========================================================================
    @Nested
    @DisplayName("indicadores")
    class Indicadores {

        @Test
        @DisplayName("contabiliza por status, prioridade e origem")
        void contabilizaPorStatusPrioridadeEOrigem() throws Exception {
            criar(tokenJoao, "Impressora não imprime", "A impressora parou e o setor inteiro está sem imprimir.");
            criar(tokenJoao, "Solicitação de acesso ao Drive", "Preciso de permissão na pasta do projeto.");

            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.porStatus.ABERTO").value(2))
                    .andExpect(jsonPath("$.porPrioridade.ALTA").value(1))
                    .andExpect(jsonPath("$.porPrioridade.BAIXA").value(1))
                    .andExpect(jsonPath("$.porOrigem.IA").value(2))
                    .andExpect(jsonPath("$.percentualClassificadoPorIa").value(100));
        }

        @Test
        @DisplayName("separa o mecanismo da triagem da origem da classificação")
        void contabilizaPorProvedorDeTriagem() throws Exception {
            criar(tokenJoao, "Impressora não imprime", "A impressora parou e o setor inteiro está sem imprimir.");
            criar(tokenJoao, "Solicitação de acesso ao Drive", "Preciso de permissão na pasta do projeto.");

            // Os dois contam como origem IA — "classificado automaticamente" —, mas
            // quem classificou foi a heuristica local. Confundir as duas coisas faria
            // o painel anunciar uso de IA que nunca aconteceu.
            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.porOrigem.IA").value(2))
                    .andExpect(jsonPath("$.porProvedorTriagem.heuristic").value(2))
                    .andExpect(jsonPath("$.porProvedorTriagem.gemini").doesNotExist());
        }

        @Test
        @DisplayName("a contagem por provedor respeita o recorte do solicitante")
        void provedorRespeitaVisibilidade() throws Exception {
            criar(tokenJoao, "Impressora não imprime", "A impressora parou e o setor inteiro está sem imprimir.");
            criar(token("beatriz.rocha@fadex.org.br"), "Erro no sistema financeiro",
                    "O módulo de pagamentos retorna erro 500.");

            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenAdmin))
                    .andExpect(jsonPath("$.porProvedorTriagem.heuristic").value(2));

            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenJoao))
                    .andExpect(jsonPath("$.porProvedorTriagem.heuristic").value(1));
        }

        @Test
        @DisplayName("o indicador de ALTA conta apenas chamados ainda não encerrados")
        void altaPrioridadeIgnoraEncerrados() throws Exception {
            long id = criar(tokenJoao, "Impressora não imprime", "A impressora parou e o setor inteiro está sem imprimir.");

            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenAdmin))
                    .andExpect(jsonPath("$.altaPrioridadeEmAberto").value(1));

            avancar(id, "EM_ANDAMENTO");
            avancar(id, "RESOLVIDO");
            avancar(id, "FECHADO");

            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenAdmin))
                    .andExpect(jsonPath("$.porPrioridade.ALTA").value(1))
                    .andExpect(jsonPath("$.altaPrioridadeEmAberto").value(0));
        }

        @Test
        @DisplayName("SOLICITANTE vê apenas os indicadores dos próprios chamados")
        void solicitanteVeApenasOsProprios() throws Exception {
            criar(tokenJoao, "Impressora não imprime", "A impressora parou e o setor inteiro está sem imprimir.");
            criar(token("beatriz.rocha@fadex.org.br"), "Erro no sistema financeiro",
                    "O módulo de pagamentos retorna erro 500.");

            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenAdmin))
                    .andExpect(jsonPath("$.total").value(2));

            mockMvc.perform(autenticado(get("/api/dashboard/metricas"), tokenJoao))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        @DisplayName("exige autenticação")
        void exigeAutenticacao() throws Exception {
            mockMvc.perform(get("/api/dashboard/metricas")).andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("eventos em tempo real")
    class Eventos {

        @Test
        @DisplayName("abrir chamado ALTA publica evento de criação com alerta")
        void chamadoAltaDisparaAlerta() throws Exception {
            criar(tokenJoao, "Impressora não imprime", "A impressora parou e o setor inteiro está sem imprimir.");

            assertThat(coletor.eventos).hasSize(1);

            ChamadoAlteradoEvento evento = coletor.eventos.get(0);
            assertThat(evento.tipo()).isEqualTo(TipoEventoRealtime.CHAMADO_CRIADO);
            assertThat(evento.alertaAltaPrioridade()).isTrue();
            assertThat(evento.chamado().prioridade().name()).isEqualTo("ALTA");
        }

        @Test
        @DisplayName("chamado de baixa prioridade não dispara alerta")
        void chamadoBaixoNaoDisparaAlerta() throws Exception {
            criar(tokenJoao, "Solicitação de acesso ao Drive", "Preciso de permissão na pasta do projeto.");

            assertThat(coletor.eventos).hasSize(1);
            assertThat(coletor.eventos.get(0).alertaAltaPrioridade()).isFalse();
        }

        @Test
        @DisplayName("mudança de status publica evento de atualização")
        void mudancaDeStatusPublicaEvento() throws Exception {
            long id = criar(tokenJoao, "Solicitação de acesso ao Drive", "Preciso de permissão na pasta do projeto.");
            coletor.limpar();

            avancar(id, "EM_ANDAMENTO");

            assertThat(coletor.eventos).hasSize(1);
            assertThat(coletor.eventos.get(0).tipo()).isEqualTo(TipoEventoRealtime.CHAMADO_ATUALIZADO);
            assertThat(coletor.eventos.get(0).chamado().status().name()).isEqualTo("EM_ANDAMENTO");
        }

        @Test
        @DisplayName("o evento identifica o solicitante, para a entrega dirigida")
        void eventoIdentificaSolicitante() throws Exception {
            criar(tokenJoao, "Solicitação de acesso ao Drive", "Preciso de permissão na pasta do projeto.");

            assertThat(coletor.eventos.get(0).solicitanteId())
                    .isEqualTo(coletor.eventos.get(0).chamado().solicitante().id());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("fluxo SSE")
    class FluxoSse {

        @Test
        @DisplayName("abre o stream e envia os indicadores atuais no primeiro evento")
        void abreStreamComEstadoInicial() throws Exception {
            mockMvc.perform(autenticado(get("/api/eventos/stream"), tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(
                            org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                                    .string("Content-Type",
                                            org.hamcrest.Matchers.containsString("text/event-stream")));
        }

        @Test
        @DisplayName("recusa conexão sem token")
        void recusaSemToken() throws Exception {
            mockMvc.perform(get("/api/eventos/stream")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("aceita o token por query param, já que o EventSource não envia cabeçalhos")
        void aceitaTokenPorQueryParam() throws Exception {
            mockMvc.perform(get("/api/eventos/stream").param("token", tokenAdmin))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("rejeita token inválido na query param")
        void rejeitaTokenInvalidoNaQuery() throws Exception {
            mockMvc.perform(get("/api/eventos/stream").param("token", "token-invalido"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Apoio
    // =========================================================================

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder autenticado(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            String token) {
        return builder.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
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

    private long criar(String token, String titulo, String descricao) throws Exception {
        String resposta =
                mockMvc.perform(autenticado(post("/api/chamados"), token)
                                .content(json(Map.of("titulo", titulo, "descricao", descricao))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private void avancar(long id, String status) throws Exception {
        mockMvc.perform(autenticado(patch("/api/chamados/" + id + "/status"), tokenAdmin)
                        .content(json(Map.of("status", status))))
                .andExpect(status().isOk());
    }
}
