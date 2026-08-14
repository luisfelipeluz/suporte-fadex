package br.org.fadex.chamados.web;

import br.org.fadex.chamados.domain.Categoria;
import br.org.fadex.chamados.domain.Papel;
import br.org.fadex.chamados.domain.Prioridade;
import br.org.fadex.chamados.domain.StatusChamado;
import br.org.fadex.chamados.repository.UsuarioRepository;
import br.org.fadex.chamados.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Listas de referencia consumidas pelos seletores da interface.
 *
 * <p>Expor categorias e status pela API — em vez de codifica-los no frontend —
 * mantem uma unica fonte de verdade e deixa a interface preparada para receber
 * novos valores sem precisar de alteracao.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Referências", description = "Listas de apoio para os filtros e seletores")
public class ReferenciaController {

    private final UsuarioRepository usuarioRepository;

    public ReferenciaController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Item generico de um seletor: valor tecnico + rotulo de exibicao. */
    public record OpcaoResponse(String valor, String rotulo, String cor) {}

    @GetMapping("/categorias")
    @Operation(summary = "Categorias disponíveis")
    public ResponseEntity<List<OpcaoResponse>> categorias() {
        return ResponseEntity.ok(
                Arrays.stream(Categoria.values())
                        .map(c -> new OpcaoResponse(c.name(), c.getRotulo(), null))
                        .toList());
    }

    @GetMapping("/prioridades")
    @Operation(summary = "Prioridades disponíveis")
    public ResponseEntity<List<OpcaoResponse>> prioridades() {
        return ResponseEntity.ok(
                Arrays.stream(Prioridade.values())
                        .map(p -> new OpcaoResponse(p.name(), p.getRotulo(), null))
                        .toList());
    }

    @GetMapping("/status")
    @Operation(summary = "Status disponíveis, com a cor usada nos badges")
    public ResponseEntity<List<OpcaoResponse>> status() {
        return ResponseEntity.ok(
                Arrays.stream(StatusChamado.values())
                        .map(s -> new OpcaoResponse(s.name(), s.getRotulo(), s.getCor()))
                        .toList());
    }

    @GetMapping("/responsaveis")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Usuários que podem assumir um chamado (ADMIN)",
            description = "Alimenta o seletor de responsável no detalhe do chamado.")
    public ResponseEntity<List<UsuarioResponse>> responsaveis() {
        return ResponseEntity.ok(
                usuarioRepository.findByPapelOrderByNomeAsc(Papel.ADMIN).stream()
                        .map(UsuarioResponse::de)
                        .toList());
    }
}
