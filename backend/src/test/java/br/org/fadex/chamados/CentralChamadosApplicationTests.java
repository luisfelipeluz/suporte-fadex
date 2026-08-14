package br.org.fadex.chamados;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de fumaca da fundacao do projeto.
 *
 * <p>Ao subir o contexto com {@code ddl-auto=validate}, este teste prova que as
 * migrations Flyway e o mapeamento JPA estao em sincronia: qualquer divergencia
 * entre uma coluna e o campo correspondente faz o contexto falhar aqui.
 */
@SpringBootTest
@ActiveProfiles("test")
class CentralChamadosApplicationTests {

    @Test
    @DisplayName("contexto sobe e o schema das migrations valida contra as entidades JPA")
    void contextoCarrega() {
        // O proprio carregamento do contexto e a assercao.
    }
}
