package br.org.fadex.chamados.realtime;

import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Conexoes SSE ativas.
 *
 * <p>Cada navegador conectado ao dashboard mantem uma inscricao aqui. A lista e
 * {@link CopyOnWriteArrayList} porque a leitura (envio de eventos) e muito mais
 * frequente que a escrita (conexao e desconexao), e porque os envios acontecem
 * em threads diferentes da que registrou a inscricao.
 *
 * <p>Toda inscricao guarda o papel do usuario: e isso que permite entregar cada
 * evento apenas a quem tem direito de ve-lo. Tempo real nao pode virar um atalho
 * para contornar a autorizacao.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    /** Conexao longa; o navegador reconecta sozinho quando ela expira. */
    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final List<Inscricao> inscricoes = new CopyOnWriteArrayList<>();

    /** Uma conexao SSE aberta por um usuario. */
    private record Inscricao(Long usuarioId, Papel papel, SseEmitter emitter) {}

    public SseEmitter registrar(Usuario usuario) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Inscricao inscricao = new Inscricao(usuario.getId(), usuario.getPapel(), emitter);

        inscricoes.add(inscricao);

        emitter.onCompletion(() -> inscricoes.remove(inscricao));
        emitter.onTimeout(
                () -> {
                    inscricoes.remove(inscricao);
                    emitter.complete();
                });
        emitter.onError(
                e -> {
                    inscricoes.remove(inscricao);
                    emitter.complete();
                });

        log.debug("Nova inscrição SSE de {} ({} ativas)", usuario.getEmail(), inscricoes.size());

        return emitter;
    }

    /** Envia somente para a equipe de suporte. */
    public void enviarParaAdmins(TipoEventoRealtime tipo, Object dados) {
        enviar(tipo, dados, i -> i.papel() == Papel.ADMIN);
    }

    /** Envia somente para um usuario especifico. */
    public void enviarParaUsuario(Long usuarioId, TipoEventoRealtime tipo, Object dados) {
        enviar(tipo, dados, i -> Objects.equals(i.usuarioId(), usuarioId));
    }

    /** Comentario de keep-alive, para atravessar proxies que fecham conexoes ociosas. */
    public void enviarBatimento() {
        for (Inscricao inscricao : inscricoes) {
            try {
                inscricao.emitter().send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException | IllegalStateException e) {
                remover(inscricao);
            }
        }
    }

    private void enviar(TipoEventoRealtime tipo, Object dados, Predicate<Inscricao> filtro) {
        for (Inscricao inscricao : inscricoes) {
            if (!filtro.test(inscricao)) {
                continue;
            }
            try {
                inscricao.emitter().send(SseEmitter.event().name(tipo.getNome()).data(dados));
            } catch (IOException | IllegalStateException e) {
                // Cliente desconectou entre a checagem e o envio: apenas descarta.
                remover(inscricao);
            }
        }
    }

    private void remover(Inscricao inscricao) {
        inscricoes.remove(inscricao);
        try {
            inscricao.emitter().complete();
        } catch (RuntimeException ignorada) {
            // Emitter ja encerrado; nada a fazer.
        }
    }

    /** Quantidade de conexoes ativas, usada em testes e diagnostico. */
    public int getConexoesAtivas() {
        return inscricoes.size();
    }
}
