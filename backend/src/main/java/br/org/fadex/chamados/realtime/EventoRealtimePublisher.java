package br.org.fadex.chamados.realtime;

import br.org.fadex.chamados.repository.UsuarioRepository;
import br.org.fadex.chamados.service.MetricasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Traduz alteracoes de chamados em eventos SSE.
 *
 * <p>O listener e {@link TransactionalEventListener} na fase
 * {@code AFTER_COMMIT} de proposito: notificar durante a transacao permitiria que
 * o dashboard recebesse um chamado que ainda pode sofrer rollback, e o painel
 * passaria a mostrar dados que nao existem no banco.
 *
 * <p>A entrega respeita o mesmo recorte de visibilidade do resto da API — o
 * SOLICITANTE recebe eventos apenas dos proprios chamados.
 */
@Component
public class EventoRealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoRealtimePublisher.class);

    private final SseEmitterRegistry registry;
    private final MetricasService metricasService;
    private final UsuarioRepository usuarioRepository;

    public EventoRealtimePublisher(
            SseEmitterRegistry registry,
            MetricasService metricasService,
            UsuarioRepository usuarioRepository) {
        this.registry = registry;
        this.metricasService = metricasService;
        this.usuarioRepository = usuarioRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAlterarChamado(ChamadoAlteradoEvento evento) {
        try {
            // A equipe de suporte acompanha a fila inteira.
            registry.enviarParaAdmins(evento.tipo(), evento.chamado());

            // O solicitante acompanha apenas o proprio chamado.
            registry.enviarParaUsuario(evento.solicitanteId(), evento.tipo(), evento.chamado());

            if (evento.alertaAltaPrioridade()) {
                registry.enviarParaAdmins(TipoEventoRealtime.ALERTA_ALTA, evento.chamado());
            }

            publicarMetricas(evento.solicitanteId());

        } catch (RuntimeException e) {
            // Falha na notificacao nao pode afetar a operacao que ja foi concluida.
            log.warn("Não foi possível publicar o evento em tempo real: {}", e.getMessage());
        }
    }

    /**
     * Recalcula e publica os indicadores.
     *
     * <p>Os numeros globais sao calculados uma unica vez para toda a equipe; o
     * solicitante recebe os indicadores restritos aos proprios chamados.
     */
    private void publicarMetricas(Long solicitanteId) {
        registry.enviarParaAdmins(TipoEventoRealtime.METRICAS, metricasService.calcularGlobal());

        usuarioRepository
                .findById(solicitanteId)
                .ifPresent(
                        solicitante ->
                                registry.enviarParaUsuario(
                                        solicitanteId,
                                        TipoEventoRealtime.METRICAS,
                                        metricasService.calcular(solicitante)));
    }

    /** Mantem as conexoes vivas atraves de proxies que encerram sessoes ociosas. */
    @Scheduled(fixedDelay = 25_000)
    public void manterConexoesVivas() {
        registry.enviarBatimento();
    }
}
