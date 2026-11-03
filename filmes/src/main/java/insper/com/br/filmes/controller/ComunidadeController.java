package insper.com.br.filmes.controller;

import insper.com.br.filmes.entity.ComentarioPublicacao;
import insper.com.br.filmes.entity.InteracaoPublicacao;
import insper.com.br.filmes.entity.Publicacao;
import insper.com.br.filmes.repository.ComentarioPublicacaoRepository;
import insper.com.br.filmes.repository.InteracaoPublicacaoRepository;
import insper.com.br.filmes.repository.PublicacaoRepository;
import insper.com.br.filmes.repository.FilmeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/comunidade/publicacoes")
public class ComunidadeController {
    private final PublicacaoRepository publicacoes;
    private final ComentarioPublicacaoRepository comentarios;
    private final InteracaoPublicacaoRepository interacoes;
    private final FilmeRepository filmes;
    public ComunidadeController(PublicacaoRepository publicacoes, ComentarioPublicacaoRepository comentarios, InteracaoPublicacaoRepository interacoes, FilmeRepository filmes) {
        this.publicacoes = publicacoes; this.comentarios = comentarios; this.interacoes = interacoes; this.filmes = filmes;
    }
    public record Pedido(@NotBlank @Size(max = 4000) String texto, String tituloFilme, Long filmeId, String categoria) {}
    public record ComentarioPedido(@NotBlank @Size(max = 2000) String texto) {}
    public record ComentarioView(Long id, String autor, String texto, Instant criadoEm) {
        static ComentarioView of(ComentarioPublicacao c) { return new ComentarioView(c.id, c.autor, c.texto, c.criadoEm); }
    }
    public record PublicacaoView(Long id, String autor, String texto, String tituloFilme, Long filmeId, String categoria, Instant criadoEm, long curtidas, long salvos, List<ComentarioView> comentarios) {}

    @GetMapping
    public List<PublicacaoView> listar() { return publicacoes.findAllByOrderByCriadoEmDesc().stream().map(this::view).toList(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacaoView publicar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Pedido pedido) {
        if (pedido.filmeId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um filme ou série do catálogo.");
        var filme = filmes.findByIdAndAtivoTrue(pedido.filmeId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filme não encontrado no catálogo."));
        Publicacao p = new Publicacao(); p.usuarioId = jwt.getSubject(); p.autor = autor(jwt); p.texto = pedido.texto().trim();
        p.tituloFilme = filme.getNome(); p.filmeId = filme.getId();
        p.categoria = "noticia".equals(pedido.categoria()) ? "noticia" : "critica"; p.criadoEm = Instant.now();
        return view(publicacoes.save(p));
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioView comentar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ComentarioPedido pedido) {
        obter(id); ComentarioPublicacao c = new ComentarioPublicacao(); c.publicacaoId = id; c.usuarioId = jwt.getSubject();
        c.autor = autor(jwt); c.texto = pedido.texto().trim(); c.criadoEm = Instant.now();
        return ComentarioView.of(comentarios.save(c));
    }

    @PutMapping("/{id}/{tipo}")
    public PublicacaoView alternar(@PathVariable Long id, @PathVariable String tipo, @AuthenticationPrincipal Jwt jwt) {
        if (!tipo.equals("curtida") && !tipo.equals("salvo")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Publicacao p = obter(id);
        var atual = interacoes.findByPublicacaoIdAndUsuarioIdAndTipo(id, jwt.getSubject(), tipo);
        if (atual.isPresent()) interacoes.delete(atual.get());
        else { InteracaoPublicacao i = new InteracaoPublicacao(); i.publicacaoId = id; i.usuarioId = jwt.getSubject(); i.tipo = tipo; interacoes.save(i); }
        return view(p);
    }

    private Publicacao obter(Long id) { return publicacoes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private String autor(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) name = jwt.getClaimAsString("nickname");
        return name == null || name.isBlank() ? "Membro da comunidade" : name;
    }
    private PublicacaoView view(Publicacao p) {
        return new PublicacaoView(p.id, p.autor, p.texto, p.tituloFilme, p.filmeId, p.categoria, p.criadoEm,
            interacoes.countByPublicacaoIdAndTipo(p.id, "curtida"), interacoes.countByPublicacaoIdAndTipo(p.id, "salvo"),
            comentarios.findByPublicacaoIdOrderByCriadoEmAsc(p.id).stream().map(ComentarioView::of).toList());
    }
}
