package insper.com.br.filmes.repository;

import insper.com.br.filmes.entity.Filme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FilmeRepository
    extends JpaRepository<Filme, Long> {

    List<Filme> findByAtivoTrueOrderByNomeAsc();

    List<Filme>
        findByAtivoTrueAndNomeStartingWithIgnoreCaseOrderByNomeAsc(
            String nome
        );

    Optional<Filme> findByIdAndAtivoTrue(Long id);
    Optional<Filme> findByChaveTmdb(String chaveTmdb);
    List<Filme> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
    Optional<Filme> findByAtivoTrueAndNomeIgnoreCaseAndLancamento(String nome, java.time.LocalDate lancamento);
}
