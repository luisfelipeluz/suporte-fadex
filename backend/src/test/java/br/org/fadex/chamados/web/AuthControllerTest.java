package br.org.fadex.chamados.web;

import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Autenticação")
class AuthControllerTest {

    private static final String SENHA_SEED = "suporte123";
    private static final String EMAIL_ADMIN = "ana.souza@fadex.org.br";
    private static final String EMAIL_SOLICITANTE = "joao.pereira@fadex.org.br";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;

    private String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }

    // =========================================================================
    @Nested
    @DisplayName("cadastro")
    class Cadastro {

        @Test
        @DisplayName("cria o usuário e devolve 201 com token")
        void criaUsuario() throws Exception {
            var corpo = Map.of(
                    "nome", "Marina Castro",
                    "email", "marina.castro@fadex.org.br",
                    "senha", "senhaSegura1");

            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.usuario.email").value("marina.castro@fadex.org.br"))
                    .andExpect(jsonPath("$.usuario.papel").value("SOLICITANTE"))
                    // o hash da senha nunca deve aparecer na resposta
                    .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
        }

        @Test
        @DisplayName("armazena a senha com hash BCrypt, nunca em texto puro")
        void armazenaSenhaComHash() throws Exception {
            var corpo = Map.of(
                    "nome", "Pedro Antunes",
                    "email", "pedro.antunes@fadex.org.br",
                    "senha", "senhaSegura1");

            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isCreated());

            Usuario salvo =
                    usuarioRepository.findByEmailIgnoreCase("pedro.antunes@fadex.org.br").orElseThrow();

            assertThat(salvo.getSenhaHash()).isNotEqualTo("senhaSegura1");
            assertThat(salvo.getSenhaHash()).startsWith("$2a$");
        }

        @Test
        @DisplayName("rejeita e-mail já cadastrado com 409")
        void rejeitaEmailDuplicado() throws Exception {
            var corpo = Map.of("nome", "Outra Ana", "email", EMAIL_ADMIN, "senha", "senhaSegura1");

            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString(EMAIL_ADMIN)));
        }

        @Test
        @DisplayName("rejeita campos obrigatórios ausentes com 400 detalhando os campos")
        void rejeitaCamposObrigatorios() throws Exception {
            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.campos").isArray())
                    .andExpect(jsonPath("$.campos.length()").value(3));
        }

        @Test
        @DisplayName("rejeita e-mail em formato inválido com 400")
        void rejeitaEmailInvalido() throws Exception {
            var corpo = Map.of("nome", "Teste", "email", "nao-e-um-email", "senha", "senhaSegura1");

            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0].campo").value("email"));
        }

        @Test
        @DisplayName("ignora tentativa de se autopromover a ADMIN pelo corpo da requisição")
        @DirtiesContext
        void naoPermiteAutopromocao() throws Exception {
            String corpo =
                    """
                    {"nome":"Invasor","email":"invasor@fadex.org.br","senha":"senhaSegura1","papel":"ADMIN"}
                    """;

            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.usuario.papel").value("SOLICITANTE"));

            Usuario salvo = usuarioRepository.findByEmailIgnoreCase("invasor@fadex.org.br").orElseThrow();
            assertThat(salvo.getPapel()).isEqualTo(Papel.SOLICITANTE);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("devolve token válido para credenciais corretas")
        void autenticaComSucesso() throws Exception {
            var corpo = Map.of("email", EMAIL_ADMIN, "senha", SENHA_SEED);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.expiraEmSegundos").isNumber())
                    .andExpect(jsonPath("$.usuario.papel").value("ADMIN"));
        }

        @Test
        @DisplayName("aceita o e-mail sem diferenciar maiúsculas")
        void aceitaEmailComCaixaDiferente() throws Exception {
            var corpo = Map.of("email", "Ana.Souza@FADEX.org.br", "senha", SENHA_SEED);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("rejeita senha incorreta com 401")
        void rejeitaSenhaIncorreta() throws Exception {
            var corpo = Map.of("email", EMAIL_ADMIN, "senha", "senha-errada");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("rejeita e-mail inexistente com 401")
        void rejeitaEmailInexistente() throws Exception {
            var corpo = Map.of("email", "ninguem@fadex.org.br", "senha", SENHA_SEED);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(corpo)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("rotas protegidas")
    class RotasProtegidas {

        @Test
        @DisplayName("sem token responde 401 no formato padrão de erro")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(get("/api/auth/eu"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.caminho").value("/api/auth/eu"));
        }

        @Test
        @DisplayName("com token válido devolve o usuário da sessão")
        void comTokenValidoRetornaUsuario() throws Exception {
            String token = autenticar(EMAIL_SOLICITANTE);

            mockMvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL_SOLICITANTE))
                    .andExpect(jsonPath("$.papel").value("SOLICITANTE"))
                    .andExpect(jsonPath("$.iniciais").value("JP"));
        }

        @Test
        @DisplayName("token malformado responde 401")
        void tokenMalformadoRetorna401() throws Exception {
            mockMvc.perform(get("/api/auth/eu").header("Authorization", "Bearer nao-e-um-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token assinado com outro segredo responde 401")
        void tokenComAssinaturaInvalidaRetorna401() throws Exception {
            // Token bem formado, porém assinado com uma chave diferente.
            String forjado =
                    "eyJhbGciOiJIUzI1NiJ9"
                            + ".eyJzdWIiOiJhbmEuc291emFAZmFkZXgub3JnLmJyIiwiZXhwIjo0MTAyNDQ0ODAwfQ"
                            + ".assinatura-invalida-gerada-com-outro-segredo";

            mockMvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + forjado))
                    .andExpect(status().isUnauthorized());
        }
    }

    /** Faz login e devolve o token, para uso nos testes de rotas protegidas. */
    private String autenticar(String email) throws Exception {
        String resposta =
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("email", email, "senha", SENHA_SEED))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(resposta).get("token").asText();
    }
}
