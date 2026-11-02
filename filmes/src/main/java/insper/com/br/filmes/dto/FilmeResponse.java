package insper.com.br.filmes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FilmeResponse(
    Long id,
    String nome,
    String diretor,
    List<String> casting,
    LocalDate lancamento,
    Map<String, String> plataformasDisponiveis,
    Integer classificacaoIndicativa,
    Integer duracao,
    String genero,
    BigDecimal avaliacao,
    String tipo,
    String sinopse,
    String imagem,
    String trailerUrl,
    List<String> trailerUrls
) {
    public FilmeResponse(Long id, String nome, String diretor, List<String> casting, LocalDate lancamento, Map<String, String> plataformasDisponiveis, Integer classificacaoIndicativa, Integer duracao, String genero, BigDecimal avaliacao) {
        this(id, nome, diretor, casting, lancamento, plataformasDisponiveis, classificacaoIndicativa, duracao, genero, avaliacao, "filme", "", "", "", List.of());
    }
}
