package insper.com.br.filmes.service;

import insper.com.br.filmes.dto.AtualizarAvaliacaoRequest;
import insper.com.br.filmes.dto.AtualizarStreamingsRequest;
import insper.com.br.filmes.dto.FilmeRequest;
import insper.com.br.filmes.dto.FilmeResponse;
import insper.com.br.filmes.entity.Filme;
import insper.com.br.filmes.exception.ResourceNotFoundException;
import insper.com.br.filmes.repository.FilmeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FilmeService {

    private final FilmeRepository filmeRepository;

    @Transactional(readOnly = true)
    public List<FilmeResponse> listar(String nome) {
        List<Filme> filmes;

        if (nome == null || nome.isBlank()) {
            filmes =
                filmeRepository.findByAtivoTrueOrderByNomeAsc();
        } else {
            filmes =
                filmeRepository
                    .findByAtivoTrueAndNomeStartingWithIgnoreCaseOrderByNomeAsc(
                        nome.trim()
                    );
        }

        return filmes.stream()
            .map(this::converterParaResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public FilmeResponse buscarPorId(Long id) {
        return converterParaResponse(
            buscarFilmeAtivo(id)
        );
    }

    @Transactional
    public FilmeResponse criar(FilmeRequest request) {
        validarUrls(request.plataformasDisponiveis());

        Filme filme = Filme.builder()
            .nome(request.nome().trim())
            .diretor(request.diretor().trim())
            .casting(request.casting())
            .lancamento(request.lancamento())
            .plataformasDisponiveis(
                request.plataformasDisponiveis()
            )
            .classificacaoIndicativa(
                request.classificacaoIndicativa()
            )
            .duracao(request.duracao())
            .genero(request.genero().trim())
            .avaliacao(request.avaliacao())
            .ativo(true)
            .build();

        Filme filmeSalvo = filmeRepository.save(filme);

        return converterParaResponse(filmeSalvo);
    }

    @Transactional
    public FilmeResponse atualizar(
        Long id,
        FilmeRequest request
    ) {
        validarUrls(request.plataformasDisponiveis());

        Filme filme = buscarFilmeAtivo(id);

        filme.setNome(request.nome().trim());
        filme.setDiretor(request.diretor().trim());
        filme.atualizarCasting(request.casting());
        filme.setLancamento(request.lancamento());
        filme.atualizarStreamings(
            request.plataformasDisponiveis()
        );
        filme.setClassificacaoIndicativa(
            request.classificacaoIndicativa()
        );
        filme.setDuracao(request.duracao());
        filme.setGenero(request.genero().trim());
        filme.atualizarAvaliacao(
            request.avaliacao()
        );

        return converterParaResponse(
            filmeRepository.save(filme)
        );
    }

    @Transactional
    public FilmeResponse atualizarAvaliacao(
        Long id,
        AtualizarAvaliacaoRequest request
    ) {
        Filme filme = buscarFilmeAtivo(id);

        filme.atualizarAvaliacao(
            request.avaliacao()
        );

        return converterParaResponse(
            filmeRepository.save(filme)
        );
    }

    @Transactional
    public FilmeResponse atualizarStreamings(
        Long id,
        AtualizarStreamingsRequest request
    ) {
        validarUrls(request.plataformasDisponiveis());

        Filme filme = buscarFilmeAtivo(id);

        filme.atualizarStreamings(
            request.plataformasDisponiveis()
        );

        return converterParaResponse(
            filmeRepository.save(filme)
        );
    }

    @Transactional(readOnly = true)
    public String obterLinkStreaming(
        Long id,
        String plataforma
    ) {
        Filme filme = buscarFilmeAtivo(id);

        return filme.redirect(plataforma);
    }

    @Transactional
    public void deletar(Long id) {
        Filme filme = buscarFilmeAtivo(id);

        filme.deletar();

        filmeRepository.save(filme);
    }

    private Filme buscarFilmeAtivo(Long id) {
        return filmeRepository
            .findByIdAndAtivoTrue(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Filme com id " + id + " não encontrado"
                )
            );
    }

    private FilmeResponse converterParaResponse(
        Filme filme
    ) {
        return new FilmeResponse(
            filme.getId(),
            filme.getNome(),
            filme.getDiretor(),
            List.copyOf(filme.getCasting()),
            filme.getLancamento(),
            Map.copyOf(
                filme.getPlataformasDisponiveis()
            ),
            filme.getClassificacaoIndicativa(),
            filme.getDuracao(),
            filme.getGenero(),
            filme.getAvaliacao()
        );
    }

    private void validarUrls(
        Map<String, String> plataformas
    ) {
        plataformas.forEach((plataforma, url) -> {
            try {
                URI uri = URI.create(url);

                boolean protocoloValido =
                    "http".equalsIgnoreCase(uri.getScheme()) ||
                    "https".equalsIgnoreCase(uri.getScheme());

                if (!protocoloValido) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                    "URL inválida para a plataforma " +
                    plataforma
                );
            }
        });
    }
}