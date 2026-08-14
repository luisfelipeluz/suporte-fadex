package br.org.fadex.chamados.duplicados;

import br.org.fadex.chamados.duplicados.SimilaridadeTextual.Comparacao;
import br.org.fadex.chamados.duplicados.SimilaridadeTextual.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Similaridade textual")
class SimilaridadeTextualTest {

    private static final String TITULO_A = "Impressora do 3º andar não imprime";
    private static final String DESCRICAO_A =
            "A impressora do 3º andar parou de imprimir e o setor inteiro está sem imprimir "
                    + "os empenhos do dia.";

    private double similaridade(String tituloA, String descricaoA, String tituloB, String descricaoB) {
        return SimilaridadeTextual.comparar(
                        SimilaridadeTextual.perfilar(tituloA, descricaoA),
                        SimilaridadeTextual.perfilar(tituloB, descricaoB))
                .score();
    }

    // =========================================================================
    @Nested
    @DisplayName("pontuação")
    class Pontuacao {

        @Test
        @DisplayName("texto idêntico atinge similaridade máxima")
        void textoIdenticoPontuaUm() {
            assertThat(similaridade(TITULO_A, DESCRICAO_A, TITULO_A, DESCRICAO_A))
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("relato do mesmo incidente com outras palavras fica acima do limiar")
        void mesmoIncidentePassaDoLimiar() {
            double score =
                    similaridade(
                            TITULO_A,
                            DESCRICAO_A,
                            "Impressora do 3º andar sem imprimir",
                            "A impressora do andar de cima não imprime os empenhos desde ontem "
                                    + "e o setor inteiro está parado.");

            assertThat(score).isGreaterThanOrEqualTo(DetectorDuplicados.LIMIAR);
        }

        @Test
        @DisplayName("incidentes sem relação ficam abaixo do limiar")
        void incidentesDiferentesFicamAbaixoDoLimiar() {
            double score =
                    similaridade(
                            TITULO_A,
                            DESCRICAO_A,
                            "Solicitação de acesso ao Drive do projeto",
                            "Novo colaborador precisa de permissão na pasta compartilhada.");

            assertThat(score).isLessThan(DetectorDuplicados.LIMIAR);
        }

        @Test
        @DisplayName("o resultado fica sempre no intervalo [0,1]")
        void resultadoNormalizado() {
            double score =
                    similaridade(
                            TITULO_A, DESCRICAO_A, TITULO_A, DESCRICAO_A + " " + DESCRICAO_A);

            assertThat(score).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("o tamanho do texto não penaliza o mesmo assunto")
        void tamanhoNaoPenaliza() {
            // O cosseno normaliza pela norma dos vetores: um relato de uma linha e
            // outro de um paragrafo sobre o mesmo problema continuam proximos.
            double score =
                    similaridade(
                            "Impressora não imprime",
                            "",
                            "Impressora não imprime",
                            "A impressora do 3º andar não imprime. A impressora apresenta luz "
                                    + "vermelha e a impressora precisa de manutenção urgente.");

            assertThat(score).isGreaterThan(0.5);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("robustez")
    class Robustez {

        @Test
        @DisplayName("é simétrica: comparar A com B é o mesmo que comparar B com A")
        void ehSimetrica() {
            Perfil a = SimilaridadeTextual.perfilar(TITULO_A, DESCRICAO_A);
            Perfil b =
                    SimilaridadeTextual.perfilar(
                            "Impressora sem imprimir", "A impressora não imprime os empenhos.");

            assertThat(SimilaridadeTextual.comparar(a, b).score())
                    .isEqualTo(SimilaridadeTextual.comparar(b, a).score());
        }

        @Test
        @DisplayName("ignora acentuação e caixa")
        void ignoraAcentuacaoECaixa() {
            double score =
                    similaridade(
                            "IMPRESSORA NÃO IMPRIME",
                            "Não imprime.",
                            "impressora nao imprime",
                            "nao imprime.");

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("texto composto apenas de cortesia não gera similaridade")
        void cortesiaNaoAproxima() {
            // Sem a remocao de stopwords, dois chamados sem nada em comum ficariam
            // proximos apenas por comecarem com "bom dia, solicito por favor".
            double score =
                    similaridade(
                            "Bom dia",
                            "Prezados, por favor, solicito a gentileza de abrir um chamado.",
                            "Boa tarde",
                            "Prezados, por favor, solicito a gentileza de abrir um chamado.");

            assertThat(score).isZero();
        }

        @Test
        @DisplayName("não quebra com texto vazio")
        void naoQuebraComTextoVazio() {
            Comparacao comparacao =
                    SimilaridadeTextual.comparar(
                            SimilaridadeTextual.perfilar("", ""),
                            SimilaridadeTextual.perfilar(TITULO_A, DESCRICAO_A));

            assertThat(comparacao.score()).isZero();
            assertThat(comparacao.termosEmComum()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("explicação")
    class Explicacao {

        @Test
        @DisplayName("informa os termos que aproximaram os dois chamados")
        void listaTermosEmComum() {
            Comparacao comparacao =
                    SimilaridadeTextual.comparar(
                            SimilaridadeTextual.perfilar(TITULO_A, DESCRICAO_A),
                            SimilaridadeTextual.perfilar(
                                    "Impressora do 3º andar parada",
                                    "A impressora não imprime os empenhos."));

            assertThat(comparacao.termosEmComum()).contains("impressora").hasSizeLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("não lista termos quando não há nada em comum")
        void semTermosQuandoNaoHaRelacao() {
            Comparacao comparacao =
                    SimilaridadeTextual.comparar(
                            SimilaridadeTextual.perfilar("Impressora parada", "Não imprime."),
                            SimilaridadeTextual.perfilar(
                                    "Reset de senha", "Preciso redefinir minha credencial."));

            assertThat(comparacao.termosEmComum()).isEmpty();
        }
    }
}
