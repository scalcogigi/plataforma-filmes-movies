package insper.com.br.filmes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> tratarNaoEncontrado(
        ResourceNotFoundException exception,
        HttpServletRequest request
    ) {
        ApiError erro = new ApiError(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Recurso não encontrado",
            exception.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(erro);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> tratarArgumentoInvalido(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        ApiError erro = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Requisição inválida",
            exception.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity
            .badRequest()
            .body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarValidacao(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> campos = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                campos.put(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );

        ApiError erro = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Erro de validação",
            "Existem campos inválidos na requisição",
            request.getRequestURI(),
            campos
        );

        return ResponseEntity
            .badRequest()
            .body(erro);
    }
}