package insper.com.br.filmes.controller;

import insper.com.br.filmes.dto.AtualizarAvaliacaoRequest;
import insper.com.br.filmes.dto.AtualizarStreamingsRequest;
import insper.com.br.filmes.dto.FilmeRequest;
import insper.com.br.filmes.dto.FilmeResponse;
import insper.com.br.filmes.service.FilmeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/filmes")
@RequiredArgsConstructor
public class FilmeController {

    private final FilmeService filmeService;

    @GetMapping
    public List<FilmeResponse> listar(
        @RequestParam(required = false) String nome
    ) {
        return filmeService.listar(nome);
    }

    @GetMapping("/{id}")
    public FilmeResponse buscarPorId(
        @PathVariable Long id
    ) {
        return filmeService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmeResponse criar(
        @Valid @RequestBody FilmeRequest request
    ) {
        return filmeService.criar(request);
    }

    @PutMapping("/{id}")
    public FilmeResponse atualizar(
        @PathVariable Long id,
        @Valid @RequestBody FilmeRequest request
    ) {
        return filmeService.atualizar(id, request);
    }

    @PatchMapping("/{id}/avaliacao")
    public FilmeResponse atualizarAvaliacao(
        @PathVariable Long id,
        @Valid
        @RequestBody AtualizarAvaliacaoRequest request
    ) {
        return filmeService.atualizarAvaliacao(
            id,
            request
        );
    }

    @PutMapping("/{id}/streamings")
    public FilmeResponse atualizarStreamings(
        @PathVariable Long id,
        @Valid
        @RequestBody AtualizarStreamingsRequest request
    ) {
        return filmeService.atualizarStreamings(
            id,
            request
        );
    }

    @GetMapping("/{id}/redirect")
    public ResponseEntity<Void> redirect(
        @PathVariable Long id,
        @RequestParam String plataforma
    ) {
        String link =
            filmeService.obterLinkStreaming(
                id,
                plataforma
            );

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(link))
            .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        filmeService.deletar(id);
    }
}