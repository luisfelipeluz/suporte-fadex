package br.org.fadex.chamados.web;

import br.org.fadex.chamados.security.UsuarioAutenticado;
import br.org.fadex.chamados.service.MetricasService;
import br.org.fadex.chamados.web.dto.MetricasResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Indicadores", description = "Métricas operacionais do painel")
public class MetricasController {

    private final MetricasService metricasService;

    public MetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/metricas")
    @Operation(
            summary = "Indicadores por status, prioridade e origem",
            description =
                    """
                    ADMIN recebe os números de toda a operação; SOLICITANTE recebe apenas os
                    dos próprios chamados.

                    Para acompanhamento contínuo, prefira o fluxo SSE em
                    `GET /api/eventos/stream`, que empurra este mesmo objeto a cada alteração.
                    """)
    @ApiResponse(responseCode = "200", description = "Indicadores calculados")
    public ResponseEntity<MetricasResponse> metricas(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ResponseEntity.ok(metricasService.calcular(autenticado.getUsuario()));
    }
}
