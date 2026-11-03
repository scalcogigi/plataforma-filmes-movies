package insper.com.br.filmes.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import insper.com.br.filmes.dto.FilmeResponse;
import insper.com.br.filmes.entity.Filme;
import insper.com.br.filmes.repository.FilmeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TmdbCatalogService {
    private final FilmeRepository repository;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final HttpClient httpClient;
    private final String baseUrl;

    @Autowired
    public TmdbCatalogService(FilmeRepository repository, ObjectMapper mapper, @Value("${tmdb.api-key:}") String apiKey) {
        this(repository, mapper, apiKey, HttpClient.newHttpClient(), "https://api.themoviedb.org/3");
    }

    TmdbCatalogService(FilmeRepository repository, ObjectMapper mapper, String apiKey, HttpClient httpClient, String baseUrl) {
        this.repository = repository;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public List<FilmeResponse> pesquisar(String termo) {
        if (termo == null || termo.isBlank() || termo.length() > 100) throw new IllegalArgumentException("Informe um título de até 100 caracteres.");
        List<Filme> encontrados = repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(termo.trim());
        if (!encontrados.isEmpty()) return encontrados.stream().map(this::response).toList();
        JsonNode results = get("/search/multi?query=" + URLEncoder.encode(termo.trim(), StandardCharsets.UTF_8));
        return importarResultados(results.path("results"), 5);
    }

    @Transactional
    public List<FilmeResponse> populares(int quantidade) {
        if (quantidade < 1 || quantidade > 40) throw new IllegalArgumentException("A quantidade deve estar entre 1 e 40.");
        List<FilmeResponse> output = new ArrayList<>();
        for (String tipo : List.of("movie", "tv")) {
            int alvo = tipo.equals("movie") ? (quantidade + 1) / 2 : quantidade;
            for (int page = 1; page <= 2 && output.size() < alvo; page++) {
                JsonNode results = get("/discover/" + tipo + "?sort_by=popularity.desc&page=" + page).path("results");
                for (JsonNode item : results) {
                    if (output.size() >= alvo) break;
                    output.add(importar(tipo, item.path("id").asLong()));
                }
            }
        }
        return output;
    }

    private List<FilmeResponse> importarResultados(JsonNode results, int limite) {
        List<FilmeResponse> output = new ArrayList<>();
        for (JsonNode item : results) {
            String tipo = item.path("media_type").asText();
            if (!tipo.equals("movie") && !tipo.equals("tv")) continue;
            output.add(importar(tipo, item.path("id").asLong()));
            if (output.size() >= limite) break;
        }
        return output;
    }

    private synchronized FilmeResponse importar(String tipo, long tmdbId) {
        String key = tipo + ":" + tmdbId;
        Filme existing = repository.findByChaveTmdb(key).orElse(null);
        if (existing != null) return response(existing);
        JsonNode data = get("/" + tipo + "/" + tmdbId + "?append_to_response=videos");
        String date = data.path(tipo.equals("tv") ? "first_air_date" : "release_date").asText();
        LocalDate lancamento;
        try { lancamento = LocalDate.parse(date); } catch (Exception ignored) { lancamento = LocalDate.of(1900, 1, 1); }
        String nome = data.path(tipo.equals("tv") ? "name" : "title").asText();
        existing = repository.findByAtivoTrueAndNomeIgnoreCaseAndLancamento(nome, lancamento).orElse(null);
        if (existing != null) return response(existing);
        String genero = data.path("genres").isArray() && !data.path("genres").isEmpty() ? data.path("genres").get(0).path("name").asText() : "Outros";
        List<String> trailers = new ArrayList<>();
        coletarTrailers(data.path("videos").path("results"), trailers);
        if (trailers.isEmpty()) coletarTrailers(get("/" + tipo + "/" + tmdbId + "/videos?language=en-US").path("results"), trailers);
        Filme film = Filme.builder()
            .chaveTmdb(key).tipo(tipo.equals("tv") ? "serie" : "filme")
            .nome(nome)
            .diretor("Não informado").casting(new ArrayList<>())
            .lancamento(lancamento).plataformasDisponiveis(new java.util.HashMap<>())
            .classificacaoIndicativa(0).duracao(Math.max(1, data.path("runtime").asInt(1)))
            .genero(genero).avaliacao(BigDecimal.valueOf(data.path("vote_average").asDouble(0)))
            .sinopse(data.path("overview").asText(""))
            .imagem(data.path("poster_path").asText().isBlank() ? "" : "https://image.tmdb.org/t/p/w500" + data.path("poster_path").asText())
            .trailerUrl(trailers.isEmpty() ? "" : trailers.get(0)).trailerUrls(trailers).ativo(true).build();
        return response(repository.save(film));
    }

    private void coletarTrailers(JsonNode videos, List<String> trailers) {
        for (JsonNode video : videos) {
            if (video.path("site").asText().equals("YouTube") && List.of("Trailer", "Teaser").contains(video.path("type").asText())) {
                String videoKey = video.path("key").asText();
                if (videoKey.matches("[A-Za-z0-9_-]{11}")) trailers.add("https://www.youtube.com/watch?v=" + videoKey);
                if (trailers.size() >= 5) break;
            }
        }
    }

    private FilmeResponse response(Filme f) {
        return new FilmeResponse(f.getId(), f.getNome(), f.getDiretor(), List.copyOf(f.getCasting()), f.getLancamento(), java.util.Map.copyOf(f.getPlataformasDisponiveis()), f.getClassificacaoIndicativa(), f.getDuracao(), f.getGenero(), f.getAvaliacao(), f.getTipo(), f.getSinopse(), f.getImagem(), f.getTrailerUrl(), f.getTrailerUrls() == null ? List.of() : List.copyOf(f.getTrailerUrls()));
    }

    private JsonNode get(String path) {
        if (apiKey.isBlank()) throw new IllegalStateException("Configure TMDB_API_KEY no Movies Service.");
        try {
            String separator = path.contains("?") ? "&" : "?";
            URI uri = URI.create(baseUrl + path + separator + "api_key=" + apiKey + (path.contains("language=") ? "" : "&language=pt-BR") + "&include_adult=false");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("TMDB respondeu com " + response.statusCode());
            return mapper.readTree(response.body());
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Consulta TMDB interrompida", e); }
        catch (Exception e) { throw new IllegalStateException("Falha na consulta TMDB: " + e.getMessage(), e); }
    }
}
