package insper.com.br.filmes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AtualizarStreamingsRequest(

    @NotNull(message = "As plataformas são obrigatórias")
    Map<
        @NotBlank(message = "O nome da plataforma é obrigatório")
        String,
        @NotBlank(message = "A URL da plataforma é obrigatória")
        String
    > plataformasDisponiveis

) {
}