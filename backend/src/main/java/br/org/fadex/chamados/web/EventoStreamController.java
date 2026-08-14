package br.org.fadex.chamados.web;

import br.org.fadex.chamados.realtime.SseEmitterRegistry;
import br.org.fadex.chamados.realtime.TipoEventoRealtime;
import br.org.fadex.chamados.security.UsuarioAutenticado;
import br.org.fadex.chamados.service.MetricasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Tempo real", description = "Fluxo de eventos via Server-Sent Events")
public class EventoStreamController {

    private final SseEmitterRegistry registry;
    private final MetricasService metricasService;

    public EventoStreamController(SseEmitterRegistry registry, MetricasService metricasService) {
        this.registry = registry;
        this.metricasService = metricasService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Abre o fluxo de eventos em tempo real",
            description =
                    """
                    Conexão SSE que empurra atualizações para o dashboard. Eventos emitidos:

                    | Evento | Quando | Quem recebe |
                    |---|---|---|
                    | `conectado` | logo após a conexão, com os indicadores atuais | quem conectou |
                    | `metricas` | a cada alteração de chamado | ADMIN (global) e o solicitante (próprios) |
                    | `chamado-criado` | abertura de chamado | ADMIN e o solicitante |
                    | `chamado-atualizado` | status, responsável, classificação ou comentário | ADMIN e o solicitante |
                    | `alerta-alta` | abertura de chamado de prioridade ALTA | somente ADMIN |

                    **Autenticação:** o `EventSource` do navegador não envia cabeçalhos
                    personalizados, então este endpoint — e apenas ele — também aceita o
                    token pelo parâmetro `token`. A validação é a mesma do cabeçalho
                    `Authorization`; nenhuma outra rota lê o token da query string.
                    """)
    @ApiResponse(responseCode = "200", description = "Fluxo de eventos aberto")
    public SseEmitter abrirFluxo(
            @Parameter(description = "Token JWT, alternativa ao cabeçalho Authorization")
                    @RequestParam(required = false)
                    String token,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        SseEmitter emitter = registry.registrar(autenticado.getUsuario());

        // Primeiro evento com o estado atual, para o painel já abrir preenchido
        // sem precisar de uma chamada REST adicional.
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(TipoEventoRealtime.CONECTADO.getNome())
                            .data(metricasService.calcular(autenticado.getUsuario())));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
