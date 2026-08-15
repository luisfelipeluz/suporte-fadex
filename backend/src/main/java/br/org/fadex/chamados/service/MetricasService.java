package br.org.fadex.chamados.service;

import br.org.fadex.chamados.domain.OrigemClassificacao;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.domain.Usuario;
import br.org.fadex.chamados.repository.ChamadoRepository;
import br.org.fadex.chamados.repository.ContagemPorProvedor;
import br.org.fadex.chamados.web.dto.MetricasResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculo dos indicadores do dashboard.
 *
 * <p>Cada consulta tem uma variante global e uma restrita ao solicitante, de modo
 * que o recorte de visibilidade aplicado a listagem valha tambem para os numeros.
 */
@Service
public class MetricasService {

    private final ChamadoRepository chamadoRepository;

    public MetricasService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    /** Indicadores no escopo do usuario: global para ADMIN, proprios para SOLICITANTE. */
    @Transactional(readOnly = true)
    public MetricasResponse calcular(Usuario usuario) {
        return calcular(usuario, usuario.isAdmin());
    }

    /**
     * Indicadores de toda a operacao, sem usuario de referencia.
     *
     * <p>Usado ao publicar metricas em tempo real para a equipe de suporte: o
     * resultado e o mesmo para todos os ADMIN, entao e calculado uma unica vez.
     */
    @Transactional(readOnly = true)
    public MetricasResponse calcularGlobal() {
        return calcular(null, true);
    }

    private MetricasResponse calcular(Usuario usuario, boolean global) {
        Map<StatusChamado, Long> porStatus = new EnumMap<>(StatusChamado.class);
        for (StatusChamado status : StatusChamado.values()) {
            porStatus.put(status, contarStatus(status, usuario, global));
        }

        Map<Prioridade, Long> porPrioridade = new EnumMap<>(Prioridade.class);
        for (Prioridade prioridade : Prioridade.values()) {
            porPrioridade.put(prioridade, contarPrioridade(prioridade, usuario, global));
        }

        Map<OrigemClassificacao, Long> porOrigem = new EnumMap<>(OrigemClassificacao.class);
        for (OrigemClassificacao origem : OrigemClassificacao.values()) {
            porOrigem.put(origem, contarOrigem(origem, usuario, global));
        }

        long total = global ? chamadoRepository.count() : chamadoRepository.countBySolicitante(usuario);

        long altaEmAberto = contarAltaEmAberto(usuario, global);

        long classificadosAutomaticamente = porOrigem.getOrDefault(OrigemClassificacao.IA, 0L);
        int percentualAutomatico =
                total == 0 ? 0 : Math.round(classificadosAutomaticamente * 100f / total);

        return new MetricasResponse(
                total,
                porStatus,
                porPrioridade,
                porOrigem,
                contarPorProvedor(usuario, global),
                altaEmAberto,
                percentualAutomatico,
                Instant.now());
    }

    /**
     * Quantos chamados cada mecanismo de triagem classificou.
     *
     * <p>Do maior para o menor, para que a interface exiba o mecanismo dominante
     * primeiro sem precisar reordenar. {@link LinkedHashMap} porque essa ordem
     * precisa sobreviver a serializacao para JSON.
     */
    private Map<String, Long> contarPorProvedor(Usuario usuario, boolean global) {
        List<ContagemPorProvedor> contagens =
                global
                        ? chamadoRepository.contarPorProvedorTriagem()
                        : chamadoRepository.contarPorProvedorTriagem(usuario);

        return contagens.stream()
                .sorted(Comparator.comparing(ContagemPorProvedor::total).reversed())
                .collect(
                        Collectors.toMap(
                                ContagemPorProvedor::provedor,
                                ContagemPorProvedor::total,
                                (a, b) -> a,
                                LinkedHashMap::new));
    }

    private long contarStatus(StatusChamado status, Usuario usuario, boolean global) {
        return global
                ? chamadoRepository.countByStatus(status)
                : chamadoRepository.countByStatusAndSolicitante(status, usuario);
    }

    private long contarPrioridade(Prioridade prioridade, Usuario usuario, boolean global) {
        return global
                ? chamadoRepository.countByPrioridade(prioridade)
                : chamadoRepository.countByPrioridadeAndSolicitante(prioridade, usuario);
    }

    private long contarOrigem(OrigemClassificacao origem, Usuario usuario, boolean global) {
        return global
                ? chamadoRepository.countByOrigemClassificacao(origem)
                : chamadoRepository.countByOrigemClassificacaoAndSolicitante(origem, usuario);
    }

    /**
     * Chamados ALTA que ainda demandam acao.
     *
     * <p>O indicador de alta prioridade do painel serve para chamar atencao da
     * equipe; contar chamados ja fechados ou cancelados o manteria alto para
     * sempre e o tornaria inutil.
     */
    private long contarAltaEmAberto(Usuario usuario, boolean global) {
        var encerrados = List.of(StatusChamado.FECHADO, StatusChamado.CANCELADO);

        return global
                ? chamadoRepository.countByPrioridadeAndStatusNotIn(Prioridade.ALTA, encerrados)
                : chamadoRepository.countByPrioridadeAndStatusNotInAndSolicitante(
                        Prioridade.ALTA, encerrados, usuario);
    }
}
