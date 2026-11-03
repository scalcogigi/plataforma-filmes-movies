package insper.com.br.filmes.config;

import insper.com.br.filmes.entity.Publicacao;
import insper.com.br.filmes.repository.PublicacaoRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class ComunidadeInicial implements ApplicationRunner {
    private final PublicacaoRepository repository;
    public ComunidadeInicial(PublicacaoRepository repository) { this.repository = repository; }
    @Override public void run(ApplicationArguments args) {
        if (repository.count() != 0) return;
        exemplo("Marina", "Duna", "Que final! A fotografia de Duna é simplesmente absurda. Denis Villeneuve constrói um mundo que parece vivo. Já quero rever no cinema!", "critica", 2);
        exemplo("Rafael", "Oppenheimer", "Essa sequência de Oppenheimer é cinema puro. A trilha, a tensão e aquilo tudo na tela... arrepiou!", "critica", 5);
        exemplo("Beatriz", "Tudo em Todo Lugar ao Mesmo Tempo", "Ainda pensando nessa cena. Que roteiro, que atuações! Tudo em Todo Lugar ao Mesmo Tempo é uma experiência única.", "critica", 24);
        exemplo("Marina", "Duna", "Villeneuve comentou que ainda existe um pedaço de Arrakis para explorar. A comunidade já está especulando o próximo filme.", "noticia", 3);
        exemplo("Rafael", "Oppenheimer", "Nolan soltou um recado curto sobre filmes em película. O post rendeu discussão a noite inteira.", "noticia", 8);
        exemplo("Beatriz", "The Last of Us", "A HBO confirmou novos episódios e os criadores já estão escolhendo a trilha da próxima temporada.", "noticia", 26);
    }
    private void exemplo(String autor, String titulo, String texto, String categoria, int horas) {
        Publicacao p = new Publicacao(); p.usuarioId = "exemplo:" + autor.toLowerCase(); p.autor = autor;
        p.tituloFilme = titulo; p.texto = texto; p.categoria = categoria; p.criadoEm = Instant.now().minusSeconds(horas * 3600L);
        repository.save(p);
    }
}
