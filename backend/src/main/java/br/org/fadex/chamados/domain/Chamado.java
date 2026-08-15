package br.org.fadex.chamados.domain;

import br.org.fadex.chamados.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chamado aberto por um solicitante.
 *
 * <p>A classificacao e guardada em dois conjuntos de campos distintos, e essa
 * separacao e proposital:
 *
 * <ul>
 *   <li>{@code categoriaSugerida} / {@code prioridadeSugerida} — a sugestao original
 *       da triagem automatica, que nunca e sobrescrita;
 *   <li>{@code categoria} / {@code prioridade} — a classificacao final vigente, que
 *       o ADMIN pode corrigir.
 * </ul>
 *
 * <p>E o que permite a interface exibir lado a lado "Sugestao da IA" e
 * "Classificacao final", e o que torna auditavel quando o suporte discordou da IA.
 */
@Entity
@Table(name = "chamado")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String descricao;

    // --- Classificacao final (vigente) ---------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_classificacao", nullable = false, length = 10)
    private OrigemClassificacao origemClassificacao;

    // --- Sugestao original da triagem automatica (imutavel) ------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_sugerida", length = 20)
    private Categoria categoriaSugerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade_sugerida", length = 10)
    private Prioridade prioridadeSugerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "confianca_ia", length = 10)
    private Confianca confiancaIa;

    @Column(name = "justificativa_ia", length = 4000)
    private String justificativaIa;

    /** Identificacao do provider que gerou a sugestao (ex.: {@code heuristic}). */
    @Column(name = "provedor_triagem", length = 60)
    private String provedorTriagem;

    /** Indica se o ADMIN ja revisou a sugestao (aceitando ou corrigindo). */
    @Column(name = "classificacao_revisada", nullable = false)
    private boolean classificacaoRevisada;

    // --- Ciclo de vida --------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChamado status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Chamado() {
        // exigido pelo JPA
    }

    public Chamado(String titulo, String descricao, Usuario solicitante) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.solicitante = solicitante;
        this.status = StatusChamado.ABERTO;
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    // --- Comportamento de dominio --------------------------------------------

    /**
     * Aplica o resultado da triagem automatica no momento da abertura.
     *
     * <p>Preenche a sugestao e adota-a como classificacao final; cabe ao ADMIN
     * revisar depois.
     */
    public void aplicarTriagem(
            Categoria categoria,
            Prioridade prioridade,
            Confianca confianca,
            String justificativa,
            String provedor) {

        this.categoriaSugerida = categoria;
        this.prioridadeSugerida = prioridade;
        this.confiancaIa = confianca;
        this.justificativaIa = justificativa;
        this.provedorTriagem = provedor;

        this.categoria = categoria;
        this.prioridade = prioridade;
        this.origemClassificacao = OrigemClassificacao.IA;
        this.classificacaoRevisada = false;
    }

    /** ADMIN confirma que a sugestao da IA esta correta. */
    public void aceitarClassificacaoIa() {
        if (classificacaoRevisada && origemClassificacao == OrigemClassificacao.IA) {
            throw new RegraDeNegocioException("A classificação deste chamado já foi aceita.");
        }
        this.categoria = categoriaSugerida;
        this.prioridade = prioridadeSugerida;
        this.origemClassificacao = OrigemClassificacao.IA;
        this.classificacaoRevisada = true;
    }

    /**
     * ADMIN substitui a classificacao sugerida. A sugestao original e preservada
     * para efeito de comparacao e auditoria.
     */
    public void corrigirClassificacao(Categoria novaCategoria, Prioridade novaPrioridade) {
        this.categoria = novaCategoria;
        this.prioridade = novaPrioridade;
        this.origemClassificacao = OrigemClassificacao.MANUAL;
        this.classificacaoRevisada = true;
    }

    /** Indica se a classificacao final difere da sugestao original da IA. */
    public boolean isClassificacaoDivergente() {
        return categoriaSugerida != null
                && (categoria != categoriaSugerida || prioridade != prioridadeSugerida);
    }

    public void atribuirResponsavel(Usuario novoResponsavel) {
        if (status.isEncerrado()) {
            throw new RegraDeNegocioException(
                    "Não é possível alterar o responsável de um chamado " + status.getRotulo().toLowerCase() + ".");
        }
        this.responsavel = novoResponsavel;
    }

    /**
     * Move o chamado no fluxo de status.
     *
     * <p>Aceita o avanco, o retorno para a etapa anterior e o encerramento direto.
     * Rejeita reabertura de chamado encerrado e salto de etapas no meio do fluxo,
     * em qualquer direcao.
     *
     * <p>Quem pode fazer cada uma dessas transicoes e decisao do servico: aqui
     * mora apenas o que e valido para o chamado, nao quem tem permissao.
     */
    public void alterarStatus(StatusChamado novoStatus) {
        if (status == StatusChamado.FECHADO) {
            throw new RegraDeNegocioException("Chamados fechados não podem ser reabertos.");
        }
        if (status == StatusChamado.CANCELADO) {
            throw new RegraDeNegocioException("Chamados cancelados não podem ter o status alterado.");
        }
        if (novoStatus == status) {
            throw new RegraDeNegocioException(
                    "O chamado já está com o status " + status.getRotulo().toLowerCase() + ".");
        }
        if (!status.permiteTransicaoPara(novoStatus)) {
            List<String> permitidos = new ArrayList<>();
            status.proximo().map(s -> "avançar para " + s.getRotulo()).ifPresent(permitidos::add);
            status.anterior().map(s -> "retornar para " + s.getRotulo()).ifPresent(permitidos::add);

            // "Avançar para Fechado" ja cobre o encerramento quando FECHADO e o
            // proximo do fluxo; repetir seria ruido na mensagem.
            if (status.permiteFechamentoDireto()
                    && status.proximo().filter(StatusChamado.FECHADO::equals).isEmpty()) {
                permitidos.add("ser fechado");
            }

            throw new RegraDeNegocioException(
                    "Transição de status inválida: de " + status.getRotulo()
                            + " o chamado só pode " + String.join(" ou ", permitidos) + ".");
        }
        this.status = novoStatus;
    }

    /** Cancelamento logico: preserva o chamado e todo o seu historico. */
    public void cancelar() {
        if (!status.permiteCancelamento()) {
            throw new RegraDeNegocioException(
                    "Este chamado já está " + status.getRotulo().toLowerCase() + " e não pode ser cancelado.");
        }
        this.status = StatusChamado.CANCELADO;
    }

    public void atualizarConteudo(String novoTitulo, String novaDescricao) {
        if (status.isEncerrado()) {
            throw new RegraDeNegocioException(
                    "Não é possível editar um chamado " + status.getRotulo().toLowerCase() + ".");
        }
        this.titulo = novoTitulo;
        this.descricao = novaDescricao;
    }

    /** Verdadeiro se o usuario informado abriu este chamado. */
    public boolean pertenceA(Usuario usuario) {
        return usuario != null
                && solicitante != null
                && Objects.equals(solicitante.getId(), usuario.getId());
    }

    // --- Acessores ------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public OrigemClassificacao getOrigemClassificacao() {
        return origemClassificacao;
    }

    public Categoria getCategoriaSugerida() {
        return categoriaSugerida;
    }

    public Prioridade getPrioridadeSugerida() {
        return prioridadeSugerida;
    }

    public Confianca getConfiancaIa() {
        return confiancaIa;
    }

    public String getJustificativaIa() {
        return justificativaIa;
    }

    public String getProvedorTriagem() {
        return provedorTriagem;
    }

    public boolean isClassificacaoRevisada() {
        return classificacaoRevisada;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Chamado outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
