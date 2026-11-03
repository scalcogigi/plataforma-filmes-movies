package insper.com.br.filmes.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "publicacoes")
public class Publicacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String usuarioId;
    @Column(nullable = false) public String autor;
    @Column(nullable = false, length = 4000) public String texto;
    public String tituloFilme;
    public Long filmeId;
    @Column(nullable = false) public String categoria;
    public Instant criadoEm;
}
