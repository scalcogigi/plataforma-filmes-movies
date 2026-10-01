package insper.com.br.filmes.support;

import insper.com.br.filmes.dto.FilmeRequest;
import insper.com.br.filmes.entity.Filme;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FilmeFixtures {
    private FilmeFixtures() { }

    public static FilmeRequest request() {
        return new FilmeRequest(" Interestelar ", " Christopher Nolan ",
                List.of("Anne Hathaway"), LocalDate.of(2014, 11, 6),
                Map.of("Netflix", "https://example.com/interestelar"),
                10, 169, " Ficção científica ", new BigDecimal("8.7"));
    }

    public static Filme filme() {
        FilmeRequest request = request();
        return Filme.builder().nome(request.nome().trim()).diretor(request.diretor().trim())
                .casting(new ArrayList<>(request.casting())).lancamento(request.lancamento())
                .plataformasDisponiveis(new HashMap<>(request.plataformasDisponiveis()))
                .classificacaoIndicativa(request.classificacaoIndicativa()).duracao(request.duracao())
                .genero(request.genero().trim()).avaliacao(request.avaliacao()).build();
    }
}
