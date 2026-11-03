package insper.com.br.filmes.repository;

import insper.com.br.filmes.entity.Publicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PublicacaoRepository extends JpaRepository<Publicacao, Long> {
    List<Publicacao> findAllByOrderByCriadoEmDesc();
}
