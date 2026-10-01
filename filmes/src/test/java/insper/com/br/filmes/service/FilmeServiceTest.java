package insper.com.br.filmes.service;

import insper.com.br.filmes.dto.*;
import insper.com.br.filmes.entity.Filme;
import insper.com.br.filmes.exception.ResourceNotFoundException;
import insper.com.br.filmes.repository.FilmeRepository;
import insper.com.br.filmes.support.FilmeFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmeServiceTest {
    @Mock FilmeRepository repository;
    @InjectMocks FilmeService service;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveListarTodosQuandoFiltroEstaVazio(String nome) {
        when(repository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(FilmeFixtures.filme()));
        assertThat(service.listar(nome)).extracting(FilmeResponse::nome).containsExactly("Interestelar");
        verify(repository, never()).findByAtivoTrueAndNomeStartingWithIgnoreCaseOrderByNomeAsc(any());
    }

    @Test
    void deveRemoverEspacosDoFiltro() {
        when(repository.findByAtivoTrueAndNomeStartingWithIgnoreCaseOrderByNomeAsc("int"))
                .thenReturn(List.of(FilmeFixtures.filme()));
        assertThat(service.listar(" int ")).hasSize(1);
        verify(repository, never()).findByAtivoTrueOrderByNomeAsc();
    }

    @Test
    void deveRetornarListaVaziaSemResultados() {
        when(repository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of());
        assertThat(service.listar(null)).isEmpty();
    }

    @Test
    void deveConverterTodosOsCamposECopiarColecoesNaConsulta() {
        Filme filme = FilmeFixtures.filme();
        filme.setId(1L);
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(filme));
        FilmeResponse result = service.buscarPorId(1L);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nome()).isEqualTo(filme.getNome());
        assertThat(result.diretor()).isEqualTo(filme.getDiretor());
        assertThat(result.casting()).containsExactlyElementsOf(filme.getCasting());
        assertThat(result.lancamento()).isEqualTo(filme.getLancamento());
        assertThat(result.plataformasDisponiveis()).isEqualTo(filme.getPlataformasDisponiveis());
        assertThat(result.classificacaoIndicativa()).isEqualTo(10);
        assertThat(result.duracao()).isEqualTo(169);
        assertThat(result.genero()).isEqualTo("Ficção científica");
        assertThat(result.avaliacao()).isEqualByComparingTo("8.7");
        filme.getCasting().clear();
        filme.getPlataformasDisponiveis().clear();
        assertThat(result.casting()).containsExactly("Anne Hathaway");
        assertThat(result.plataformasDisponiveis()).containsKey("Netflix");
    }

    @Test
    void deveCriarFilmeAtivoComTextosSemEspacos() {
        when(repository.save(any(Filme.class))).thenAnswer(invocation -> {
            Filme filme = invocation.getArgument(0);
            assertThat(filme.getAtivo()).isTrue();
            filme.setId(7L);
            return filme;
        });
        FilmeResponse result = service.criar(FilmeFixtures.request());
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.nome()).isEqualTo("Interestelar");
        assertThat(result.diretor()).isEqualTo("Christopher Nolan");
        assertThat(result.genero()).isEqualTo("Ficção científica");
        assertThat(result.casting()).containsExactly("Anne Hathaway");
        assertThat(result.plataformasDisponiveis()).containsEntry("Netflix", "https://example.com/interestelar");
        verify(repository).save(any(Filme.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://example.com/filme", "https://example.com/filme", "HTTPS://example.com/filme"})
    void deveAceitarUrlsHttpEHttps(String url) {
        when(repository.save(any(Filme.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.criar(comPlataformas(Map.of("Max", url))).plataformasDisponiveis())
                .containsEntry("Max", url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com/filme", "/filme", "https://url com espaco", ""})
    void deveRejeitarUrlInvalidaAntesDeSalvar(String url) {
        assertThatThrownBy(() -> service.criar(comPlataformas(Map.of("Max", url))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("URL inválida para a plataforma Max");
        verifyNoInteractions(repository);
    }

    @Test
    void devePermitirCadastroSemPlataformas() {
        when(repository.save(any(Filme.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.criar(comPlataformas(Map.of())).plataformasDisponiveis()).isEmpty();
    }

    @Test
    void deveAtualizarTodosOsDadosPreservandoId() {
        Filme filme = FilmeFixtures.filme();
        filme.setId(1L);
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(filme));
        when(repository.save(filme)).thenReturn(filme);
        FilmeRequest request = new FilmeRequest(" Novo filme ", " Nova diretora ", List.of("Novo ator"),
                java.time.LocalDate.of(2020, 1, 2), Map.of("Max", "http://example.com/novo"),
                12, 120, " Drama ", new BigDecimal("9.0"));

        FilmeResponse result = service.atualizar(1L, request);

        assertThat(result).isEqualTo(new FilmeResponse(1L, "Novo filme", "Nova diretora", request.casting(),
                request.lancamento(), request.plataformasDisponiveis(), 12, 120, "Drama", request.avaliacao()));
        assertThat(filme.getAtivo()).isTrue();
        verify(repository).save(filme);
    }

    @Test
    void deveAtualizarSomenteAvaliacao() {
        Filme filme = FilmeFixtures.filme();
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(filme));
        when(repository.save(filme)).thenReturn(filme);
        FilmeResponse result = service.atualizarAvaliacao(1L, new AtualizarAvaliacaoRequest(BigDecimal.TEN));
        assertThat(result.avaliacao()).isEqualByComparingTo("10");
        assertThat(result.nome()).isEqualTo("Interestelar");
        assertThat(result.plataformasDisponiveis()).containsKey("Netflix");
        verify(repository).save(filme);
    }

    @Test
    void deveSubstituirStreamingsERemoverPlataformasAnteriores() {
        Filme filme = FilmeFixtures.filme();
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(filme));
        when(repository.save(filme)).thenReturn(filme);
        Map<String, String> plataformas = Map.of("Max", "https://example.com/novo");
        assertThat(service.atualizarStreamings(1L, new AtualizarStreamingsRequest(plataformas))
                .plataformasDisponiveis()).isEqualTo(plataformas);
        assertThat(filme.getNome()).isEqualTo("Interestelar");
        verify(repository).save(filme);
    }

    @Test
    void deveRejeitarUrlInvalidaNasDuasFormasDeAtualizacao() {
        Map<String, String> plataformas = Map.of("Max", "ftp://example.com");
        assertThatThrownBy(() -> service.atualizar(1L, comPlataformas(plataformas)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.atualizarStreamings(1L, new AtualizarStreamingsRequest(plataformas)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void deveRedirecionarIgnorandoMaiusculasEMinusculas() {
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(FilmeFixtures.filme()));
        assertThat(service.obterLinkStreaming(1L, "nEtFlIx")).isEqualTo("https://example.com/interestelar");
    }

    @Test
    void deveRejeitarPlataformaIndisponivel() {
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(FilmeFixtures.filme()));
        assertThatThrownBy(() -> service.obterLinkStreaming(1L, "Max"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O filme não está disponível na plataforma informada");
    }

    @Test
    void deveExcluirLogicamenteSemRemoverRegistro() {
        Filme filme = FilmeFixtures.filme();
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(filme));
        service.deletar(1L);
        assertThat(filme.getAtivo()).isFalse();
        verify(repository).save(filme);
        verify(repository, never()).delete(any(Filme.class));
    }

    @Test
    void deveRejeitarOperacoesSobreFilmeInexistente() {
        assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Filme com id 99 não encontrado");
        assertThatThrownBy(() -> service.atualizar(99L, FilmeFixtures.request()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.atualizarAvaliacao(99L, new AtualizarAvaliacaoRequest(BigDecimal.ONE)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.atualizarStreamings(99L, new AtualizarStreamingsRequest(Map.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.obterLinkStreaming(99L, "Netflix"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    private FilmeRequest comPlataformas(Map<String, String> plataformas) {
        FilmeRequest r = FilmeFixtures.request();
        return new FilmeRequest(r.nome(), r.diretor(), r.casting(), r.lancamento(), plataformas,
                r.classificacaoIndicativa(), r.duracao(), r.genero(), r.avaliacao());
    }
}
