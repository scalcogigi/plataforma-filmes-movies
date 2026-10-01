package insper.com.br.filmes.repository;

import insper.com.br.filmes.entity.Filme;
import insper.com.br.filmes.support.FilmeFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FilmeRepositoryTest {
    @Autowired FilmeRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    void devePersistirFilmeComElencoEPlataformas() {
        Filme filme = repository.saveAndFlush(FilmeFixtures.filme());
        Long id = filme.getId();
        entityManager.clear();
        Filme loaded = repository.findByIdAndAtivoTrue(id).orElseThrow();
        assertThat(loaded).isNotSameAs(filme);
        assertThat(loaded.getCasting()).containsExactly("Anne Hathaway");
        assertThat(loaded.getPlataformasDisponiveis()).containsEntry("Netflix", "https://example.com/interestelar");
        assertThat(loaded.getAvaliacao()).isEqualByComparingTo("8.7");
        assertThat(loaded.getLancamento()).isEqualTo(filme.getLancamento());
    }

    @Test
    void deveListarSomenteAtivosEmOrdemAlfabetica() {
        salvar("Zodíaco", true);
        salvar("Avatar", true);
        salvar("Excluído", false);
        assertThat(repository.findByAtivoTrueOrderByNomeAsc())
                .extracting(Filme::getNome).containsExactly("Avatar", "Zodíaco");
    }

    @Test
    void deveFiltrarPorPrefixoSemDiferenciarCaixaEExcluirInativos() {
        salvar("Interestelar", true);
        salvar("Indiana Jones", true);
        salvar("O Incrível", true);
        salvar("Inativo", false);
        assertThat(repository.findByAtivoTrueAndNomeStartingWithIgnoreCaseOrderByNomeAsc("iN"))
                .extracting(Filme::getNome).containsExactly("Indiana Jones", "Interestelar");
        assertThat(repository.findByAtivoTrueAndNomeStartingWithIgnoreCaseOrderByNomeAsc("ausente")).isEmpty();
    }

    @Test
    void deveOcultarExcluidoSemApagarRegistro() {
        Filme filme = salvar("Interestelar", true);
        filme.deletar();
        repository.saveAndFlush(filme);
        entityManager.clear();
        assertThat(repository.findByIdAndAtivoTrue(filme.getId())).isEmpty();
        assertThat(repository.findById(filme.getId())).get().extracting(Filme::getAtivo).isEqualTo(false);
        assertThat(repository.findByIdAndAtivoTrue(Long.MAX_VALUE)).isEmpty();
    }

    private Filme salvar(String nome, boolean ativo) {
        Filme filme = FilmeFixtures.filme();
        filme.setNome(nome);
        filme.setAtivo(ativo);
        return repository.saveAndFlush(filme);
    }
}
