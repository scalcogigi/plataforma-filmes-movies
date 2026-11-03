package insper.com.br.filmes.service;

import insper.com.br.filmes.entity.Filme;
import insper.com.br.filmes.repository.FilmeRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TmdbCatalogServiceTest {
    @Test
    void importaFilmeESerieComSinopseImagemETrailersSemRede() throws Exception {
        FilmeRepository repository = mock(FilmeRepository.class);
        HttpClient client = mock(HttpClient.class);
        when(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Ferro"))
            .thenReturn(List.of());
        when(repository.findByChaveTmdb(anyString())).thenReturn(Optional.empty());
        when(repository.findByAtivoTrueAndNomeIgnoreCaseAndLancamento(anyString(), any(LocalDate.class)))
            .thenReturn(Optional.empty());
        when(repository.save(any(Filme.class))).thenAnswer(call -> call.getArgument(0));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(call -> {
            HttpRequest request = call.getArgument(0);
            String path = request.uri().getPath();
            String json = switch (path) {
                case "/search/multi" -> "{\"results\":[{\"media_type\":\"person\",\"id\":3},{\"media_type\":\"movie\",\"id\":1},{\"media_type\":\"tv\",\"id\":2}]}";
                case "/movie/1" -> "{\"title\":\"Homem de Ferro\",\"release_date\":\"2008-05-02\",\"genres\":[{\"name\":\"Ação\"}],\"overview\":\"Uma armadura.\",\"poster_path\":\"/poster.jpg\",\"vote_average\":8.1,\"runtime\":126,\"videos\":{\"results\":[{\"site\":\"YouTube\",\"type\":\"Trailer\",\"key\":\"abcdefghijk\"},{\"site\":\"Vimeo\",\"type\":\"Trailer\",\"key\":\"badbadbadab\"}]}}";
                case "/tv/2" -> "{\"name\":\"Série Ferro\",\"first_air_date\":\"2020-02-03\",\"overview\":\"Uma série.\",\"genres\":[],\"videos\":{\"results\":[]}}";
                case "/tv/2/videos" -> "{\"results\":[{\"site\":\"YouTube\",\"type\":\"Teaser\",\"key\":\"123456789ab\"}]}";
                case "/discover/movie" -> "{\"results\":[{\"id\":1}]}";
                case "/discover/tv" -> "{\"results\":[{\"id\":2}]}";
                default -> throw new AssertionError("Requisição inesperada: " + request.uri());
            };
            @SuppressWarnings("unchecked") HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(response.body()).thenReturn(json);
            return response;
        });
        var service = new TmdbCatalogService(repository, new ObjectMapper(), "chave-de-teste", client, "https://example.test");
        var result = service.pesquisar("Ferro");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).nome()).isEqualTo("Homem de Ferro");
        assertThat(result.get(0).sinopse()).isEqualTo("Uma armadura.");
        assertThat(result.get(0).imagem()).endsWith("/poster.jpg");
        assertThat(result.get(0).trailerUrls()).containsExactly("https://www.youtube.com/watch?v=abcdefghijk");
        assertThat(result.get(1).tipo()).isEqualTo("serie");
        assertThat(result.get(1).trailerUrls()).containsExactly("https://www.youtube.com/watch?v=123456789ab");
        assertThat(service.populares(2)).hasSize(2);
        verify(repository, times(4)).save(any(Filme.class));
    }

    @Test
    void validaBuscaELimiteDeImportacaoAntesDaConsultaExterna() {
        FilmeRepository repository = mock(FilmeRepository.class);
        var service = new TmdbCatalogService(repository, new ObjectMapper(), "");
        assertThatThrownBy(() -> service.pesquisar(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.pesquisar("x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.populares(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.populares(41)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.pesquisar("Filme ausente"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("TMDB_API_KEY");
        verify(repository).findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Filme ausente");
    }

    @Test
    void pesquisaPrimeiroNoBancoComTrailersSemChamarServicoExterno() {
        FilmeRepository repository = mock(FilmeRepository.class);
        Filme filme = Filme.builder().id(12L).nome("Homem de Ferro").tipo("filme")
            .diretor("Jon Favreau").casting(List.of("Robert Downey Jr."))
            .lancamento(LocalDate.of(2008, 5, 2)).plataformasDisponiveis(Map.of())
            .classificacaoIndicativa(12).duracao(126).genero("Ação")
            .avaliacao(BigDecimal.valueOf(8.0)).sinopse("Um herói cria uma armadura.")
            .trailerUrls(List.of("https://www.youtube.com/watch?v=abcdefghijk")).build();
        when(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Homem de Ferro")).thenReturn(List.of(filme));
        var service = new TmdbCatalogService(repository, new ObjectMapper(), "");
        var result = service.pesquisar("Homem de Ferro");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).trailerUrls()).containsExactly("https://www.youtube.com/watch?v=abcdefghijk");
        verify(repository).findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Homem de Ferro");
        verifyNoMoreInteractions(repository);
    }
}
