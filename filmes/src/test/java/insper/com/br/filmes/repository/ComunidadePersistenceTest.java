package insper.com.br.filmes.repository;

import insper.com.br.filmes.entity.ComentarioPublicacao;
import insper.com.br.filmes.entity.InteracaoPublicacao;
import insper.com.br.filmes.entity.Publicacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ComunidadePersistenceTest {
    @Autowired PublicacaoRepository publicacoes;
    @Autowired ComentarioPublicacaoRepository comentarios;
    @Autowired InteracaoPublicacaoRepository interacoes;

    @Test
    void salvaPublicacaoComentarioECurtidaRelacionados() {
        Publicacao p = new Publicacao(); p.usuarioId = "auth0|ana"; p.autor = "Ana";
        p.texto = "Gostei de Duna"; p.tituloFilme = "Duna"; p.categoria = "critica"; p.criadoEm = Instant.now();
        p = publicacoes.save(p);
        ComentarioPublicacao c = new ComentarioPublicacao(); c.publicacaoId = p.id;
        c.usuarioId = "auth0|bia"; c.autor = "Bia"; c.texto = "Também gostei"; c.criadoEm = Instant.now();
        comentarios.save(c);
        InteracaoPublicacao i = new InteracaoPublicacao(); i.publicacaoId = p.id; i.usuarioId = "auth0|bia"; i.tipo = "curtida";
        interacoes.save(i);
        assertThat(comentarios.findByPublicacaoIdOrderByCriadoEmAsc(p.id)).extracting(x -> x.texto).containsExactly("Também gostei");
        assertThat(interacoes.countByPublicacaoIdAndTipo(p.id, "curtida")).isEqualTo(1);
        assertThat(interacoes.findByPublicacaoIdAndUsuarioIdAndTipo(p.id, "auth0|bia", "curtida")).isPresent();
    }
}
