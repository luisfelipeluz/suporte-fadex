package br.org.fadex.chamados.triage;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Confianca;
import br.org.fadex.chamados.domain.Prioridade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Triagem heurística")
class HeuristicTriageProviderTest {

    private final HeuristicTriageProvider provider = new HeuristicTriageProvider();

    private TriageResult classificar(String titulo, String descricao) {
        return provider.classificar(new TriageRequest(titulo, descricao));
    }

    // =========================================================================
    @Nested
    @DisplayName("categoria sugerida")
    class CategoriaSugerida {

        @ParameterizedTest(name = "\"{0}\" -> {2}")
        @CsvSource({
            "Impressora não está funcionando, A impressora do 3º andar não imprime desde ontem., HARDWARE",
            "Notebook não liga, O notebook do setor de projetos não liga nem carregando., HARDWARE",
            "Internet indisponível no bloco B, Sem conexão cabeada e Wi-Fi instável no bloco B., REDE",
            "VPN cai a cada 10 minutos, A VPN desconecta ao acessar os sistemas internos., REDE",
            "Solicitação de acesso ao e-mail, Novo colaborador precisa de caixa de e-mail institucional., ACESSO",
            "Reset de senha do portal, O link de recuperação de senha não chega., ACESSO",
            "Erro ao acessar sistema financeiro, O módulo de pagamentos retorna erro 500., SISTEMAS",
            "Falha no sistema de folha, O relatório de folha fecha com divergência de valores., SISTEMAS",
            "Instalar leitor de PDF assinável, Preciso de um leitor de PDF para assinar termos., SOFTWARE",
            "Planilha travando ao abrir, A planilha congela ao abrir as abas de anexos., SOFTWARE"
        })
        @DisplayName("identifica a categoria a partir do vocabulário do texto")
        void identificaCategoria(String titulo, String descricao, Categoria esperada) {
            assertThat(classificar(titulo, descricao).categoria()).isEqualTo(esperada);
        }

        @Test
        @DisplayName("cai em OUTROS com confiança baixa quando não há padrão reconhecível")
        void semPadraoVaiParaOutros() {
            TriageResult resultado = classificar("Dúvida", "Gostaria de conversar com a equipe.");

            assertThat(resultado.categoria()).isEqualTo(Categoria.OUTROS);
            assertThat(resultado.confianca()).isEqualTo(Confianca.BAIXA);
            assertThat(resultado.justificativa()).contains("Nenhum padrão dominante");
        }

        @Test
        @DisplayName("o título pesa mais que a descrição quando há termos concorrentes")
        void tituloPesaMais() {
            // "impressora" no titulo (peso 3) vence "sistema" citado de passagem (peso 1).
            TriageResult resultado =
                    classificar("Impressora com defeito", "Preciso registrar isso no sistema.");

            assertThat(resultado.categoria()).isEqualTo(Categoria.HARDWARE);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("prioridade sugerida")
    class PrioridadeSugerida {

        @Test
        @DisplayName("bloqueio total resulta em prioridade ALTA")
        void bloqueioResultaEmAlta() {
            TriageResult resultado =
                    classificar(
                            "Sistema de protocolo fora do ar",
                            "A tela de protocolo não carrega e retorna erro de conexão.");

            assertThat(resultado.prioridade()).isEqualTo(Prioridade.ALTA);
            assertThat(resultado.justificativa()).contains("prioridade ALTA");
        }

        @Test
        @DisplayName("degradação com contorno resulta em prioridade MÉDIA")
        void degradacaoResultaEmMedia() {
            TriageResult resultado =
                    classificar(
                            "Editor de texto lento",
                            "O editor está lento ao salvar arquivos grandes.");

            assertThat(resultado.prioridade()).isEqualTo(Prioridade.MEDIA);
        }

        @Test
        @DisplayName("solicitação de rotina resulta em prioridade BAIXA")
        void rotinaResultaEmBaixa() {
            TriageResult resultado =
                    classificar(
                            "Solicitação de acesso ao Drive do projeto",
                            "Preciso de permissão na pasta do projeto de extensão.");

            assertThat(resultado.prioridade()).isEqualTo(Prioridade.BAIXA);
        }

        @Test
        @DisplayName("amplitude do impacto eleva a prioridade em um nível")
        void amplitudeElevaPrioridade() {
            TriageResult semAmplitude =
                    classificar("Editor lento", "O editor está lento ao salvar.");
            TriageResult comAmplitude =
                    classificar(
                            "Editor lento",
                            "O editor está lento ao salvar e afeta o setor inteiro.");

            assertThat(semAmplitude.prioridade()).isEqualTo(Prioridade.MEDIA);
            assertThat(comAmplitude.prioridade()).isEqualTo(Prioridade.ALTA);
            assertThat(comAmplitude.justificativa()).contains("múltiplos usuários");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("robustez")
    class Robustez {

        @Test
        @DisplayName("é determinística: a mesma entrada produz sempre a mesma saída")
        void ehDeterministica() {
            String titulo = "Impressora não está funcionando";
            String descricao = "A impressora do 3º andar não imprime e o setor inteiro parou.";

            TriageResult primeira = classificar(titulo, descricao);
            TriageResult segunda = classificar(titulo, descricao);

            assertThat(primeira).isEqualTo(segunda);
        }

        @Test
        @DisplayName("ignora acentuação e caixa")
        void ignoraAcentuacaoECaixa() {
            TriageResult comAcento = classificar("Impressora NÃO LIGA", "Não liga.");
            TriageResult semAcento = classificar("impressora nao liga", "nao liga.");

            assertThat(comAcento.categoria()).isEqualTo(semAcento.categoria());
            assertThat(comAcento.prioridade()).isEqualTo(semAcento.prioridade());
        }

        @Test
        @DisplayName("não quebra com texto vazio")
        void naoQuebraComTextoVazio() {
            TriageResult resultado = classificar("", "");

            assertThat(resultado.categoria()).isEqualTo(Categoria.OUTROS);
            assertThat(resultado.prioridade()).isEqualTo(Prioridade.BAIXA);
            assertThat(resultado.justificativa()).isNotBlank();
        }

        @Test
        @DisplayName("sempre identifica o provider que classificou")
        void informaProvider() {
            assertThat(classificar("Notebook não liga", "Não liga.").provedor())
                    .isEqualTo(HeuristicTriageProvider.NOME);
        }

        @Test
        @DisplayName("sempre devolve uma justificativa legível")
        void sempreJustifica() {
            TriageResult resultado =
                    classificar("Impressora não imprime", "O setor inteiro está sem imprimir.");

            assertThat(resultado.justificativa())
                    .isNotBlank()
                    .contains("Hardware")
                    .contains("prioridade ALTA");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("confiança")
    class ConfiancaDaSugestao {

        @Test
        @DisplayName("vocabulário forte e inequívoco gera confiança ALTA")
        void vocabularioForteGeraConfiancaAlta() {
            TriageResult resultado =
                    classificar(
                            "Impressora não imprime",
                            "A impressora está com erro e o setor inteiro parou de imprimir.");

            assertThat(resultado.confianca()).isEqualTo(Confianca.ALTA);
        }

        @Test
        @DisplayName("texto sem sinal algum gera confiança BAIXA")
        void textoVagoGeraConfiancaBaixa() {
            assertThat(classificar("Ajuda", "Preciso de ajuda com uma coisa.").confianca())
                    .isEqualTo(Confianca.BAIXA);
        }
    }
}
