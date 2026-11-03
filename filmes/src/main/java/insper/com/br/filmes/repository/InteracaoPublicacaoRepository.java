package insper.com.br.filmes.repository;

import insper.com.br.filmes.entity.InteracaoPublicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InteracaoPublicacaoRepository extends JpaRepository<InteracaoPublicacao, Long> {
    long countByPublicacaoIdAndTipo(Long publicacaoId, String tipo);
    Optional<InteracaoPublicacao> findByPublicacaoIdAndUsuarioIdAndTipo(Long publicacaoId, String usuarioId, String tipo);
}
