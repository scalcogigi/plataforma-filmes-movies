package insper.com.br.filmes.exception;

public class ResourceNotFoundException
    extends RuntimeException {

    public ResourceNotFoundException(String mensagem) {
        super(mensagem);
    }
}