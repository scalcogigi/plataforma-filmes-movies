package insper.com.br.filmes.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "avaliacoes", uniqueConstraints = @UniqueConstraint(columnNames = {"filme_id", "usuario_id"}))
public class Avaliacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "filme_id", nullable = false)
    public Long filmeId;
    @Column(name = "usuario_id", nullable = false)
    public String usuarioId;
    @Column(nullable = false, precision = 3, scale = 1)
    public BigDecimal nota;
    @Column(length = 2000)
    public String comentario;
    public Instant atualizadoEm;
}
