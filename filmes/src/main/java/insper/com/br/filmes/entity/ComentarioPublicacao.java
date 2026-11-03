package insper.com.br.filmes.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comentarios_publicacao")
public class ComentarioPublicacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long publicacaoId;
    @Column(nullable = false) public String usuarioId;
    @Column(nullable = false) public String autor;
    @Column(nullable = false, length = 2000) public String texto;
    public Instant criadoEm;
}
