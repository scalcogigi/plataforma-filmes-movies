package insper.com.br.filmes.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtualizarAvaliacaoRequest(

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