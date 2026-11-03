package insper.com.br.filmes.controller;

import insper.com.br.filmes.repository.AvaliacaoRepository;
import insper.com.br.filmes.repository.ComentarioPublicacaoRepository;
import insper.com.br.filmes.repository.FilmeRepository;
import insper.com.br.filmes.repository.InteracaoPublicacaoRepository;
import insper.com.br.filmes.repository.PublicacaoRepository;
import insper.com.br.filmes.support.FilmeFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComunidadeAvaliacaoControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FilmeRepository filmes;
    @Autowired PublicacaoRepository publicacoes;
    @Autowired ComentarioPublicacaoRepository comentarios;
    @Autowired InteracaoPublicacaoRepository interacoes;
    @Autowired AvaliacaoRepository avaliacoes;

    @BeforeEach
    void limpar() {
        interacoes.deleteAll();
        comentarios.deleteAll();
        publicacoes.deleteAll();
        avaliacoes.deleteAll();
        filmes.deleteAll();
    }

    @Test
    void publicarComentarCurtirSalvarEListar() throws Exception {
        long filmeId = filmes.save(FilmeFixtures.filme()).getId();
        String body = mvc.perform(post("/comunidade/publicacoes")
                .with(jwt().jwt(j -> j.subject("auth0|marina").claim("name", "Marina")))
                .contentType("application/json")
                .content("{\"texto\":\" Gostei muito \" ,\"filmeId\":" + filmeId + ",\"categoria\":\"critica\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.autor").value("Marina"))
            .andExpect(jsonPath("$.texto").value("Gostei muito"))
            .andExpect(jsonPath("$.tituloFilme").value("Interestelar"))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("id").asLong();

        mvc.perform(post("/comunidade/publicacoes/{id}/comentarios", id).with(jwt())
                .contentType("application/json").content("{\"texto\":\" Concordo \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.texto").value("Concordo"));
        mvc.perform(put("/comunidade/publicacoes/{id}/curtida", id).with(jwt()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.curtidas").value(1));
        mvc.perform(put("/comunidade/publicacoes/{id}/salvo", id).with(jwt()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.salvos").value(1));
        mvc.perform(get("/comunidade/publicacoes"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].comentarios[0].texto").value("Concordo"));
        mvc.perform(put("/comunidade/publicacoes/{id}/curtida", id).with(jwt()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.curtidas").value(0));
        assertThat(publicacoes.count()).isEqualTo(1);
    }

    @Test
    void validarPublicacaoEProtecaoDeEscrita() throws Exception {
        mvc.perform(post("/comunidade/publicacoes").contentType("application/json")
                .content("{\"texto\":\"Oi\",\"filmeId\":1}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/comunidade/publicacoes").with(jwt())
                .contentType("application/json").content("{\"texto\":\"Oi\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/comunidade/publicacoes").with(jwt())
                .contentType("application/json").content("{\"texto\":\"Oi\",\"filmeId\":999}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/comunidade/publicacoes/999/invalido").with(jwt()))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/comunidade/publicacoes/999/comentarios").with(jwt())
                .contentType("application/json").content("{\"texto\":\"Oi\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void avaliarAtualizarEConsultarFilme() throws Exception {
        long filmeId = filmes.save(FilmeFixtures.filme()).getId();
        mvc.perform(get("/filmes/{id}/avaliacoes", filmeId))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(put("/filmes/{id}/avaliacoes/me", filmeId).with(jwt())
                .contentType("application/json").content("{\"nota\":8.5,\"comentario\":\"Bom\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nota").value(8.5));
        mvc.perform(put("/filmes/{id}/avaliacoes/me", filmeId).with(jwt())
                .contentType("application/json").content("{\"nota\":9,\"comentario\":\"Ótimo\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.comentario").value("Ótimo"));
        mvc.perform(get("/filmes/{id}/avaliacoes", filmeId))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].usuarioId").value("user"));
        assertThat(avaliacoes.count()).isEqualTo(1);
        mvc.perform(get("/filmes/999/avaliacoes")).andExpect(status().isNotFound());
        mvc.perform(get("/catalogo/pesquisar").param("termo", "Interestelar"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Interestelar"));
    }
}
