package insper.com.br.filmes.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:5173, https://plataforma-filmes-frontend.vercel.app")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {
    @Autowired MockMvc mvc;

    @Test
    void devePermitirPreflightDasOrigensConfiguradasSemToken() throws Exception {
        for (String origin : new String[]{"http://localhost:5173", "https://plataforma-filmes-frontend.vercel.app"}) {
            mvc.perform(options("/filmes/1/avaliacoes/me").header("Origin", origin)
                            .header("Access-Control-Request-Method", "PUT")
                            .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    void deveRecusarOrigemNaoConfigurada() throws Exception {
        mvc.perform(options("/filmes/1/avaliacoes/me").header("Origin", "https://outro.example")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void deveDistinguirTokenAusenteDeErroDeCorsNasGravacoes() throws Exception {
        mvc.perform(put("/filmes/1/avaliacoes/me").header("Origin", "https://plataforma-filmes-frontend.vercel.app")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://plataforma-filmes-frontend.vercel.app"));
    }
}
