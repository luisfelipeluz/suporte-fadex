package br.org.fadex.chamados.domain;

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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro imutavel de algo que aconteceu com um chamado.
 *
 * <p>Alimenta a timeline cronologica exibida no detalhe. Diferente do comentario,
 * nao e escrito pelo usuario: e gerado pelo sistema a cada mudanca relevante
 * (abertura, classificacao, atribuicao, mudanca de status, cancelamento).
 */
@Entity
@Table(name = "evento_historico")
public class EventoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    /** Nulo quando o evento foi produzido pelo proprio sistema (ex.: triagem da IA). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEvento tipo;

    @Column(nullable = false, length = 500)
    private String descricao;

    /** Rotulo curto opcional exibido ao lado do evento (ex.: "CONFIANÇA ALTA"). */
    @Column(length = 60)
    private String etiqueta;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected EventoHistorico() {
        // exigido pelo JPA
    }

    public EventoHistorico(Chamado chamado, Usuario autor, TipoEvento tipo, String descricao) {
        this(chamado, autor, tipo, descricao, null);
    }

    public EventoHistorico(
            Chamado chamado, Usuario autor, TipoEvento tipo, String descricao, String etiqueta) {
        this.chamado = chamado;
        this.autor = autor;
        this.tipo = tipo;
        this.descricao = descricao;
        this.etiqueta = etiqueta;
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Chamado getChamado() {
        return chamado;
    }

    public Usuario getAutor() {
        return autor;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventoHistorico outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
