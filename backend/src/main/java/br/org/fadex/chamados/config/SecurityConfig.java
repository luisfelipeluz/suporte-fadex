package br.org.fadex.chamados.config;

import br.org.fadex.chamados.security.JwtAuthenticationFilter;
import br.org.fadex.chamados.security.TratadorErroSeguranca;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracao de seguranca da API.
 *
 * <p>A API e stateless: nao ha sessao no servidor, cada requisicao carrega o seu
 * token. {@code @EnableMethodSecurity} habilita {@code @PreAuthorize} nos
 * controllers, de modo que a restricao por papel fique declarada junto da
 * operacao que ela protege.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Rotas acessiveis sem autenticacao. */
    private static final String[] ROTAS_PUBLICAS = {
        "/api/auth/login",
        "/api/auth/registrar",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TratadorErroSeguranca tratadorErroSeguranca;
    private final CorsProperties corsProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            TratadorErroSeguranca tratadorErroSeguranca,
            CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tratadorErroSeguranca = tratadorErroSeguranca;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Desnecessario em API stateless com token no cabecalho: nao ha
                // cookie de sessao que o navegador envie automaticamente.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        req ->
                                req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                                        .anyRequest().authenticated())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(tratadorErroSeguranca)
                                        .accessDeniedHandler(tratadorErroSeguranca))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(corsProperties.allowedOrigins());
        configuracao.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }

    /**
     * BCrypt: algoritmo com salt embutido e custo ajustavel, de modo que a senha
     * nunca e armazenada nem comparavel em texto puro.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao)
            throws Exception {
        return configuracao.getAuthenticationManager();
    }
}
