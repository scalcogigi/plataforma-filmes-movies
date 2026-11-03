package insper.com.br.filmes.repository;

import insper.com.br.filmes.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByFilmeIdOrderByAtualizadoEmDesc(Long filmeId);
    Optional<Avaliacao> findByFilmeIdAndUsuarioId(Long filmeId, String usuarioId);
}
