package insper.com.br.filmes.controller;

import insper.com.br.filmes.dto.FilmeResponse;
import insper.com.br.filmes.service.TmdbCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/catalogo")
public class CatalogoController {
    private final TmdbCatalogService service;
    public CatalogoController(TmdbCatalogService service) { this.service = service; }

    @GetMapping("/pesquisar")
    public List<FilmeResponse> pesquisar(@RequestParam String termo) { return service.pesquisar(termo); }

    @PostMapping("/populares")
    public List<FilmeResponse> populares(@RequestParam(defaultValue = "20") int quantidade) { return service.populares(quantidade); }
}
