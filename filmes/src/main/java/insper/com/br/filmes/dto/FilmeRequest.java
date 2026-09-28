package insper.com.br.filmes.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FilmeRequest(

    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotBlank(message = "O diretor é obrigatório")
    String diretor,

    @NotNull(message = "O casting é obrigatório")
    @NotEmpty(message = "O casting não pode estar vazio")
    List<
        @NotBlank(message = "O nome do ator não pode estar vazio")
        String
    > casting,

    @NotNull(message = "A data de lançamento é obrigatória")
    LocalDate lancamento,

    @NotNull(message = "As plataformas são obrigatórias")
    Map<
        @NotBlank(message = "O nome da plataforma é obrigatório")
        String,
        @NotBlank(message = "A URL da plataforma é obrigatória")
        String
    > plataformasDisponiveis,

    @NotNull(message = "A classificação indicativa é obrigatória")
    @Min(
        value = 0,
        message = "A classificação indicativa não pode ser negativa"
    )
    Integer classificacaoIndicativa,

    @NotNull(message = "A duração é obrigatória")
    @Positive(message = "A duração deve ser maior que zero")
    Integer duracao,

    @NotBlank(message = "O gênero é obrigatório")
    String genero,

    @NotNull(message = "A avaliação é obrigatória")
    @DecimalMin(
        value = "0.0",
        message = "A avaliação mínima é 0"
    )
    @DecimalMax(
        value = "10.0",
        message = "A avaliação máxima é 10"
    )
    BigDecimal avaliacao

) {
}