package insper.com.br.filmes.config;

import insper.com.br.filmes.repository.FilmeRepository;
import insper.com.br.filmes.service.TmdbCatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CatalogoInicial implements ApplicationRunner {
    private final TmdbCatalogService tmdb;
    private final FilmeRepository filmes;
    private final int quantidade;

    public CatalogoInicial(TmdbCatalogService tmdb, FilmeRepository filmes, @Value("${catalog.seed-count:0}") int quantidade) {
        this.tmdb = tmdb;
        this.filmes = filmes;
        this.quantidade = quantidade;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (quantidade > 0 && filmes.count() == 0) tmdb.populares(quantidade);
    }
}
