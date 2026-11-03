package insper.com.br.filmes.repository;

import insper.com.br.filmes.entity.ComentarioPublicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComentarioPublicacaoRepository extends JpaRepository<ComentarioPublicacao, Long> {
    List<ComentarioPublicacao> findByPublicacaoIdOrderByCriadoEmAsc(Long publicacaoId);
    long countByPublicacaoId(Long publicacaoId);
}
