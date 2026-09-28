package insper.com.br.filmes.entity;

import insper.com.br.filmes.redirect.Redirect;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "filmes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Filme implements Redirect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String diretor;

    @ElementCollection
    @CollectionTable(
        name = "filme_casting",
        joinColumns = @JoinColumn(name = "filme_id")
    )
    @Column(name = "ator")
    @Builder.Default
    private List<String> casting = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate lancamento;

    @ElementCollection
    @CollectionTable(
        name = "filme_streamings",
        joinColumns = @JoinColumn(name = "filme_id")
    )
    @MapKeyColumn(name = "plataforma")
    @Column(name = "url")
    @Builder.Default
    private Map<String, String> plataformasDisponiveis = new HashMap<>();

    @Column(nullable = false)
    private Integer classificacaoIndicativa;

    @Column(nullable = false)
    private Integer duracao;

    @Column(nullable = false)
    private String genero;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal avaliacao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    public void atualizarCasting(List<String> novoCasting) {
        casting.clear();
        casting.addAll(novoCasting);
    }

    public void atualizarStreamings(
        Map<String, String> novasPlataformas
    ) {
        plataformasDisponiveis.clear();
        plataformasDisponiveis.putAll(novasPlataformas);
    }

    public void atualizarAvaliacao(BigDecimal novaAvaliacao) {
        avaliacao = novaAvaliacao;
    }

    public void deletar() {
        ativo = false;
    }

    @Override
    public String redirect(String plataforma) {
        return plataformasDisponiveis.entrySet()
            .stream()
            .filter(entry ->
                entry.getKey().equalsIgnoreCase(plataforma)
            )
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "O filme não está disponível na plataforma informada"
                )
            );
    }
}