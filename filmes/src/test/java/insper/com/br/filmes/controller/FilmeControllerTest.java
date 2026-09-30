package insper.com.br.filmes.controller;

import insper.com.br.filmes.dto.FilmeRequest;
import insper.com.br.filmes.entity.Filme;
import insper.com.br.filmes.repository.FilmeRepository;
import insper.com.br.filmes.support.FilmeFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FilmeControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FilmeRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void deveExecutarCicloCompletoComPersistenciaEExclusaoLogica() throws Exception {
        String body = mvc.perform(post("/filmes").contentType("application/json")
                        .content(mapper.writeValueAsString(FilmeFixtures.request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Interestelar"))
                .andExpect(jsonPath("$.diretor").value("Christopher Nolan"))
                .andExpect(jsonPath("$.casting[0]").value("Anne Hathaway"))
                .andExpect(jsonPath("$.lancamento").value("2014-11-06"))
                .andExpect(jsonPath("$.plataformasDisponiveis.Netflix").value("https://example.com/interestelar"))
                .andExpect(jsonPath("$.classificacaoIndicativa").value(10))
                .andExpect(jsonPath("$.duracao").value(169))
                .andExpect(jsonPath("$.genero").value("Ficção científica"))
                .andExpect(jsonPath("$.avaliacao").value(8.7))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("id").asLong();

        mvc.perform(get("/filmes/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Interestelar"));
        mvc.perform(get("/filmes").param("nome", " iNt ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/filmes").param("nome", "ausente")).andExpect(status().isOk())
                .andExpect(content().json("[]"));

        FilmeRequest r = FilmeFixtures.request();
        FilmeRequest updated = new FilmeRequest(" Novo filme ", " Nova diretora ", java.util.List.of("Novo ator"),
                java.time.LocalDate.of(2020, 1, 2), Map.of("Max", "https://example.com/novo"),
                12, 120, " Drama ", new java.math.BigDecimal("9.0"));
        mvc.perform(put("/filmes/{id}", id).contentType("application/json")
                        .content(mapper.writeValueAsString(updated)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Novo filme"))
                .andExpect(jsonPath("$.diretor").value("Nova diretora"))
                .andExpect(jsonPath("$.casting[0]").value("Novo ator"))
                .andExpect(jsonPath("$.lancamento").value("2020-01-02"))
                .andExpect(jsonPath("$.classificacaoIndicativa").value(12))
                .andExpect(jsonPath("$.duracao").value(120))
                .andExpect(jsonPath("$.genero").value("Drama"))
                .andExpect(jsonPath("$.avaliacao").value(9.0))
                .andExpect(jsonPath("$.plataformasDisponiveis.Netflix").doesNotExist());
        mvc.perform(get("/filmes/{id}", id)).andExpect(jsonPath("$.nome").value("Novo filme"));

        mvc.perform(patch("/filmes/{id}/avaliacao", id).contentType("application/json")
                        .content("{\"avaliacao\":10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avaliacao").value(10));
        mvc.perform(put("/filmes/{id}/streamings", id).contentType("application/json")
                        .content("{\"plataformasDisponiveis\":{\"Prime\":\"https://example.com/prime\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plataformasDisponiveis.Prime").value("https://example.com/prime"))
                .andExpect(jsonPath("$.plataformasDisponiveis.Max").doesNotExist());
        mvc.perform(get("/filmes/{id}/redirect", id).param("plataforma", "pRiMe"))
                .andExpect(status().isFound()).andExpect(header().string("Location", "https://example.com/prime"));
        mvc.perform(get("/filmes/{id}", id)).andExpect(jsonPath("$.avaliacao").value(10))
                .andExpect(jsonPath("$.plataformasDisponiveis.Prime").value("https://example.com/prime"));

        mvc.perform(delete("/filmes/{id}", id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(repository.findById(id)).get().extracting(Filme::getAtivo).isEqualTo(false);
        mvc.perform(get("/filmes")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/filmes/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/filmes/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(patch("/filmes/{id}/avaliacao", id).contentType("application/json")
                .content("{\"avaliacao\":5}")).andExpect(status().isNotFound());
        mvc.perform(put("/filmes/{id}", id).contentType("application/json")
                .content(mapper.writeValueAsString(r))).andExpect(status().isNotFound());
        mvc.perform(put("/filmes/{id}/streamings", id).contentType("application/json")
                .content("{\"plataformasDisponiveis\":{}}")).andExpect(status().isNotFound());
        mvc.perform(get("/filmes/{id}/redirect", id).param("plataforma", "Prime"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornarErroEstruturadoParaFilmeInexistente() throws Exception {
        mvc.perform(get("/filmes/99999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.mensagem").value("Filme com id 99999 não encontrado"))
                .andExpect(jsonPath("$.caminho").value("/filmes/99999"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty()).andExpect(jsonPath("$.campos").isEmpty());
    }

    @Test
    void deveRejeitarPlataformaIndisponivel() throws Exception {
        long id = repository.save(FilmeFixtures.filme()).getId();
        mvc.perform(get("/filmes/{id}/redirect", id).param("plataforma", "ausente"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                .andExpect(jsonPath("$.mensagem").value("O filme não está disponível na plataforma informada"))
                .andExpect(jsonPath("$.caminho").value("/filmes/" + id + "/redirect"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "diretor", "casting", "lancamento", "plataformasDisponiveis",
            "classificacaoIndicativa", "duracao", "genero", "avaliacao"})
    void deveRejeitarCampoObrigatorioAusenteNoCadastroENaAtualizacao(String field) throws Exception {
        ObjectNode json = mapper.valueToTree(FilmeFixtures.request());
        json.remove(field);
        String body = mapper.writeValueAsString(json);
        mvc.perform(post("/filmes").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos." + field).isNotEmpty())
                .andExpect(jsonPath("$.erro").value("Erro de validação"));
        mvc.perform(put("/filmes/1").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos." + field).isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "diretor", "genero", "casting", "duracao", "classificacaoIndicativa", "avaliacao"})
    void deveRejeitarValoresInvalidosNoCadastro(String field) throws Exception {
        ObjectNode json = mapper.valueToTree(FilmeFixtures.request());
        switch (field) {
            case "casting" -> json.putArray(field);
            case "duracao" -> json.put(field, 0);
            case "classificacaoIndicativa", "avaliacao" -> json.put(field, -1);
            default -> json.put(field, "   ");
        }
        mvc.perform(post("/filmes").contentType("application/json").content(mapper.writeValueAsString(json)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos." + field).isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "-0.1", "10.1"})
    void deveRejeitarAvaliacaoForaDosLimitesOuNula(String value) throws Exception {
        long id = repository.save(FilmeFixtures.filme()).getId();
        mvc.perform(patch("/filmes/{id}/avaliacao", id).contentType("application/json")
                        .content("{\"avaliacao\":" + value + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.avaliacao").isNotEmpty());
        assertThat(repository.findById(id).orElseThrow().getAvaliacao()).isEqualByComparingTo("8.7");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "10"})
    void deveAceitarLimitesDaAvaliacao(String value) throws Exception {
        long id = repository.save(FilmeFixtures.filme()).getId();
        mvc.perform(patch("/filmes/{id}/avaliacao", id).contentType("application/json")
                        .content("{\"avaliacao\":" + value + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avaliacao").value(Integer.parseInt(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"plataformasDisponiveis\":null}",
            "{\"plataformasDisponiveis\":{\"\":\"https://example.com\"}}",
            "{\"plataformasDisponiveis\":{\"Netflix\":\" \"}}"})
    void deveValidarNomeEUrlDosStreamings(String body) throws Exception {
        mvc.perform(put("/filmes/1/streamings").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value("Erro de validação"))
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }

    @Test
    void deveRejeitarProtocoloInvalidoSemAlterarStreamings() throws Exception {
        long id = repository.save(FilmeFixtures.filme()).getId();
        mvc.perform(put("/filmes/{id}/streamings", id).contentType("application/json")
                        .content("{\"plataformasDisponiveis\":{\"Max\":\"ftp://example.com\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("URL inválida para a plataforma Max"));
        mvc.perform(get("/filmes/{id}", id)).andExpect(jsonPath("$.plataformasDisponiveis.Netflix").exists())
                .andExpect(jsonPath("$.plataformasDisponiveis.Max").doesNotExist());
    }

    @Test
    void devePermitirRemoverTodosOsStreamings() throws Exception {
        long id = repository.save(FilmeFixtures.filme()).getId();
        mvc.perform(put("/filmes/{id}/streamings", id).contentType("application/json")
                        .content("{\"plataformasDisponiveis\":{}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plataformasDisponiveis").isEmpty());
        mvc.perform(get("/filmes/{id}/redirect", id).param("plataforma", "Netflix"))
                .andExpect(status().isBadRequest());
    }
}
