package br.org.fadex.chamados;

import br.org.fadex.chamados.domain.OrigemClassificacao;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.repository.ChamadoRepository;
import br.org.fadex.chamados.repository.ComentarioRepository;
import br.org.fadex.chamados.repository.EventoHistoricoRepository;
import br.org.fadex.chamados.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida a carga de demonstracao.
 *
 * <p>A suite normal roda apenas {@code db/migration}, sem os dados de vitrine.
 * Este teste carrega tambem {@code db/demo}, em um banco separado, para garantir
 * que a migration de demonstracao seja SQL valido e coerente — caso contrario o
 * erro so apareceria quando o avaliador subisse a aplicacao.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
            "spring.flyway.locations=classpath:db/migration,classpath:db/demo",
            "spring.datasource.url=jdbc:h2:mem:seed_demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                    + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        })
@DisplayName("Carga de demonstração")
class SeedDemonstracaoTest {

    @Autowired private ChamadoRepository chamadoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ComentarioRepository comentarioRepository;
    @Autowired private EventoHistoricoRepository eventoRepository;

    @Test
    @DisplayName("cria usuários, chamados, histórico e comentários consistentes")
    void cargaEhValida() {
        assertThat(usuarioRepository.count()).isEqualTo(9);
        assertThat(chamadoRepository.count()).isEqualTo(12);
        assertThat(comentarioRepository.count()).isEqualTo(4);

        // Todo chamado tem, no mínimo, abertura e classificação registradas.
        assertThat(eventoRepository.count()).isGreaterThanOrEqualTo(24);
    }

    @Test
    @DisplayName("cobre todos os status e prioridades, para o painel abrir representativo")
    void cobreTodosOsEstados() {
        assertThat(chamadoRepository.countByStatus(StatusChamado.ABERTO)).isEqualTo(3);
        assertThat(chamadoRepository.countByStatus(StatusChamado.EM_ANDAMENTO)).isEqualTo(3);
        assertThat(chamadoRepository.countByStatus(StatusChamado.RESOLVIDO)).isEqualTo(3);
        assertThat(chamadoRepository.countByStatus(StatusChamado.FECHADO)).isEqualTo(3);

        assertThat(chamadoRepository.countByPrioridade(Prioridade.ALTA)).isEqualTo(4);
        assertThat(chamadoRepository.countByPrioridade(Prioridade.MEDIA)).isEqualTo(4);
        assertThat(chamadoRepository.countByPrioridade(Prioridade.BAIXA)).isEqualTo(4);
    }

    @Test
    @DisplayName("inclui chamados classificados pela IA e corrigidos manualmente")
    void incluiAmbasAsOrigens() {
        assertThat(chamadoRepository.countByOrigemClassificacao(OrigemClassificacao.IA)).isEqualTo(10);
        assertThat(chamadoRepository.countByOrigemClassificacao(OrigemClassificacao.MANUAL)).isEqualTo(2);
    }

    @Test
    @DisplayName("todo chamado tem solicitante e a sugestão original preservada")
    void integridadeDosChamados() {
        chamadoRepository
                .findAll()
                .forEach(
                        chamado -> {
                            assertThat(chamado.getSolicitante()).isNotNull();
                            assertThat(chamado.getCategoriaSugerida()).isNotNull();
                            assertThat(chamado.getPrioridadeSugerida()).isNotNull();
                            assertThat(chamado.getJustificativaIa()).isNotBlank();
                            assertThat(chamado.getProvedorTriagem()).isEqualTo("heuristic");
                        });
    }
}
