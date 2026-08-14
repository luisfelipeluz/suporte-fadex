package br.org.fadex.chamados.duplicados;

import br.org.fadex.chamados.domain.Chamado;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.repository.ChamadoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Aponta chamados que provavelmente relatam o mesmo incidente.
 *
 * <p>Responde ao diferencial "deteccao de chamados duplicados/similares" do
 * desafio. O ganho operacional e concreto: quando um servico cai, a central recebe
 * o mesmo incidente varias vezes em poucos minutos, e a equipe precisa perceber
 * isso <b>antes</b> de abrir tres frentes de trabalho para a mesma causa.
 *
 * <h2>Recorte de visibilidade</h2>
 *
 * <p>A sugestao respeita exatamente a mesma regra de autorizacao da listagem: o
 * ADMIN compara com todos os chamados, o SOLICITANTE apenas com os proprios.
 * Fosse diferente, a deteccao viraria um canal lateral para ler o titulo e a
 * descricao de chamados de terceiros — a funcionalidade nao pode furar a
 * autorizacao que o resto do sistema aplica.
 *
 * <h2>Custo</h2>
 *
 * <p>A comparacao roda em memoria, entao o conjunto candidato e limitado por dois
 * lados: uma janela de {@value #JANELA_DIAS} dias e um teto de
 * {@value #MAXIMO_CANDIDATOS} chamados mais recentes. Um chamado de tres meses
 * atras nao e um duplicado — e historico.
 */
@Service
public class DetectorDuplicados {

    /**
     * Similaridade minima para que dois chamados sejam apresentados como possivel
     * duplicata.
     *
     * <p>Calibrado para o comportamento real do cosseno sobre textos curtos: abaixo
     * disso o que sobra e coincidencia de vocabulario de dominio ("sistema",
     * "acesso") entre chamados sem relacao. Preferiu-se um limiar que erra por
     * omissao: um falso positivo desperdica a atencao do suporte, enquanto um
     * falso negativo apenas mantem o comportamento que o sistema ja tinha.
     */
    static final double LIMIAR = 0.35;

    /** Quantas sugestoes exibir. Mais do que isso deixa de ser uma dica e vira ruido. */
    static final int MAXIMO_SUGESTOES = 5;

    static final int JANELA_DIAS = 60;

    static final int MAXIMO_CANDIDATOS = 500;

    /** Chamado cancelado nao interessa: nao ha trabalho a consolidar com ele. */
    private static final List<StatusChamado> STATUS_IGNORADOS = List.of(StatusChamado.CANCELADO);

    private final ChamadoRepository chamadoRepository;

    public DetectorDuplicados(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    /**
     * Chamados semelhantes ao informado, do mais parecido para o menos.
     *
     * @param alvo chamado que se quer comparar
     * @param observador usuario que vera o resultado; define o recorte de visibilidade
     * @return lista possivelmente vazia, nunca nula
     */
    @Transactional(readOnly = true)
    public List<ChamadoSimilar> similaresA(Chamado alvo, Usuario observador) {
        SimilaridadeTextual.Perfil perfilAlvo =
                SimilaridadeTextual.perfilar(alvo.getTitulo(), alvo.getDescricao());

        if (perfilAlvo.vazio()) {
            // Texto composto so de stopwords: nao ha o que comparar.
            return List.of();
        }

        return candidatos(observador).stream()
                .filter(candidato -> !Objects.equals(candidato.getId(), alvo.getId()))
                .map(candidato -> avaliar(perfilAlvo, candidato))
                .filter(similar -> similar.score() >= LIMIAR)
                .sorted(
                        Comparator.comparingDouble(ChamadoSimilar::score)
                                .reversed()
                                .thenComparing(
                                        similar -> similar.chamado().getCriadoEm(),
                                        Comparator.reverseOrder()))
                .limit(MAXIMO_SUGESTOES)
                .toList();
    }

    private List<Chamado> candidatos(Usuario observador) {
        Instant desde = Instant.now().minus(JANELA_DIAS, ChronoUnit.DAYS);
        PageRequest limite = PageRequest.ofSize(MAXIMO_CANDIDATOS);

        return observador.isAdmin()
                ? chamadoRepository.findByStatusNotInAndCriadoEmAfterOrderByCriadoEmDesc(
                        STATUS_IGNORADOS, desde, limite)
                : chamadoRepository
                        .findByStatusNotInAndCriadoEmAfterAndSolicitanteOrderByCriadoEmDesc(
                                STATUS_IGNORADOS, desde, observador, limite);
    }

    private ChamadoSimilar avaliar(SimilaridadeTextual.Perfil perfilAlvo, Chamado candidato) {
        SimilaridadeTextual.Comparacao comparacao =
                SimilaridadeTextual.comparar(
                        perfilAlvo,
                        SimilaridadeTextual.perfilar(
                                candidato.getTitulo(), candidato.getDescricao()));

        return new ChamadoSimilar(candidato, comparacao.score(), comparacao.termosEmComum());
    }
}
