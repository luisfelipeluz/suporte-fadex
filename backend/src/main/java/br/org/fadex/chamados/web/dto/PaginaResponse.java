package br.org.fadex.chamados.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginacao proprio.
 *
 * <p>Evita expor o JSON do {@code Page} do Spring Data, que carrega detalhes
 * internos (como {@code pageable} e {@code sort}) e cuja forma varia entre
 * versoes — o frontend passa a depender de um contrato estavel e enxuto.
 */
@Schema(name = "Pagina")
public record PaginaResponse<T>(
        List<T> conteudo,
        @Schema(example = "0") int pagina,
        @Schema(example = "10") int tamanho,
        @Schema(example = "42") long totalElementos,
        @Schema(example = "5") int totalPaginas,
        boolean primeira,
        boolean ultima) {

    public static <E, D> PaginaResponse<D> de(Page<E> pagina, Function<E, D> conversor) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast());
    }
}
