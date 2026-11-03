package insper.com.br.filmes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "interacoes_publicacao", uniqueConstraints = @UniqueConstraint(columnNames = {"publicacao_id", "usuario_id", "tipo"}))
public class InteracaoPublicacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long publicacaoId;
    @Column(nullable = false) public String usuarioId;
    @Column(nullable = false) public String tipo;
}
