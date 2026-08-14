package br.org.fadex.chamados.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentacao OpenAPI da API.
 *
 * <p>Declara o esquema de seguranca {@code bearerAuth} para que o botao
 * "Authorize" do Swagger UI permita colar o token obtido em {@code /api/auth/login}
 * e testar todos os endpoints protegidos diretamente pelo navegador.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI centralChamadosOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Central de Chamados FADEX")
                                .version("1.0.0")
                                .description(
                                        """
                                        API REST de uma central de chamados internos (help desk) com \
                                        **triagem automática por IA** e **indicadores em tempo real**.

                                        ### Como testar
                                        1. Autentique-se em `POST /api/auth/login` com uma das credenciais de teste.
                                        2. Copie o campo `token` da resposta.
                                        3. Clique em **Authorize** (canto superior direito) e cole o token.

                                        ### Credenciais de teste (criadas pela migration de seed)
                                        | Papel | E-mail | Senha |
                                        |---|---|---|
                                        | ADMIN | `ana.souza@fadex.org.br` | `suporte123` |
                                        | SOLICITANTE | `joao.pereira@fadex.org.br` | `suporte123` |
                                        """)
                                .contact(new Contact().name("Luis Felipe Luz"))
                                .license(new License().name("Desafio técnico FADEX")))
                .servers(
                        List.of(
                                new Server().url("/").description("Servidor atual")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        ESQUEMA_JWT,
                                        new SecurityScheme()
                                                .name(ESQUEMA_JWT)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Token JWT devolvido por POST /api/auth/login")));
    }
}
