package insper.com.br.filmes.controller;

import insper.com.br.filmes.entity.Avaliacao;
import insper.com.br.filmes.repository.AvaliacaoRepository;
import insper.com.br.filmes.repository.FilmeRepository;
import insper.com.br.filmes.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/filmes/{filmeId}/avaliacoes")
public class AvaliacaoController {
    private final AvaliacaoRepository avaliacoes;
    private final FilmeRepository filmes;
    public AvaliacaoController(AvaliacaoRepository avaliacoes, FilmeRepository filmes) { this.avaliacoes = avaliacoes; this.filmes = filmes; }

    public record Pedido(@NotNull @DecimalMin("0") @DecimalMax("10") BigDecimal nota, @Size(max = 2000) String comentario) {}
    public record Resposta(Long id, Long filmeId, String usuarioId, BigDecimal nota, String comentario, Instant atualizadoEm) {
        public static Resposta from(Avaliacao a) { return new Resposta(a.id, a.filmeId, a.usuarioId, a.nota, a.comentario, a.atualizadoEm); }
    }

    @GetMapping
    public List<Resposta> listar(@PathVariable Long filmeId) {
        validarFilme(filmeId);
        return avaliacoes.findByFilmeIdOrderByAtualizadoEmDesc(filmeId).stream().map(Resposta::from).toList();
    }

    @PutMapping("/me")
    public Resposta salvar(@PathVariable Long filmeId, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Pedido pedido) {
        validarFilme(filmeId);
        Avaliacao a = avaliacoes.findByFilmeIdAndUsuarioId(filmeId, jwt.getSubject()).orElseGet(Avaliacao::new);
        a.filmeId = filmeId; a.usuarioId = jwt.getSubject(); a.nota = pedido.nota(); a.comentario = pedido.comentario(); a.atualizadoEm = Instant.now();
        return Resposta.from(avaliacoes.save(a));
    }

    private void validarFilme(Long id) { if (filmes.findByIdAndAtivoTrue(id).isEmpty()) throw new ResourceNotFoundException("Filme não encontrado"); }
}
