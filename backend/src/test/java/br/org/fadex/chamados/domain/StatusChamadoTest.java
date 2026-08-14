package br.org.fadex.chamados.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras do fluxo de status, que sustentam as validacoes exigidas pelo desafio. */
class StatusChamadoTest {

    @Nested
    @DisplayName("fluxo sequencial")
    class Fluxo {

        @Test
        @DisplayName("avanca uma etapa por vez: ABERTO -> EM_ANDAMENTO -> RESOLVIDO -> FECHADO")
        void avancaUmaEtapaPorVez() {
            assertThat(StatusChamado.ABERTO.permiteTransicaoPara(StatusChamado.EM_ANDAMENTO)).isTrue();
            assertThat(StatusChamado.EM_ANDAMENTO.permiteTransicaoPara(StatusChamado.RESOLVIDO)).isTrue();
            assertThat(StatusChamado.RESOLVIDO.permiteTransicaoPara(StatusChamado.FECHADO)).isTrue();
        }

        @Test
        @DisplayName("nao permite pular etapas")
        void naoPermitePularEtapas() {
            assertThat(StatusChamado.ABERTO.permiteTransicaoPara(StatusChamado.RESOLVIDO)).isFalse();
            assertThat(StatusChamado.ABERTO.permiteTransicaoPara(StatusChamado.FECHADO)).isFalse();
        }

        @Test
        @DisplayName("nao permite retroceder")
        void naoPermiteRetroceder() {
            assertThat(StatusChamado.RESOLVIDO.permiteTransicaoPara(StatusChamado.ABERTO)).isFalse();
            assertThat(StatusChamado.EM_ANDAMENTO.permiteTransicaoPara(StatusChamado.ABERTO)).isFalse();
        }
    }

    @Nested
    @DisplayName("estados terminais")
    class Terminais {

        @Test
        @DisplayName("chamado FECHADO nao pode ser reaberto para nenhum status")
        void fechadoNaoReabre() {
            for (StatusChamado destino : StatusChamado.values()) {
                assertThat(StatusChamado.FECHADO.permiteTransicaoPara(destino))
                        .as("FECHADO -> %s", destino)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("chamado CANCELADO tambem e terminal")
        void canceladoEhTerminal() {
            for (StatusChamado destino : StatusChamado.values()) {
                assertThat(StatusChamado.CANCELADO.permiteTransicaoPara(destino)).isFalse();
            }
        }

        @Test
        @DisplayName("estados terminais nao admitem cancelamento")
        void terminaisNaoCancelam() {
            assertThat(StatusChamado.FECHADO.permiteCancelamento()).isFalse();
            assertThat(StatusChamado.CANCELADO.permiteCancelamento()).isFalse();
            assertThat(StatusChamado.ABERTO.permiteCancelamento()).isTrue();
            assertThat(StatusChamado.EM_ANDAMENTO.permiteCancelamento()).isTrue();
            assertThat(StatusChamado.RESOLVIDO.permiteCancelamento()).isTrue();
        }

        @Test
        @DisplayName("proximo() e vazio nos estados terminais")
        void proximoVazioNosTerminais() {
            assertThat(StatusChamado.FECHADO.proximo()).isEmpty();
            assertThat(StatusChamado.CANCELADO.proximo()).isEmpty();
            assertThat(StatusChamado.ABERTO.proximo()).contains(StatusChamado.EM_ANDAMENTO);
        }
    }
}
