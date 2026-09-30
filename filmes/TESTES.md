# Testes e cobertura

Requer JDK 21 ou superior configurado em `JAVA_HOME`.
Na pasta `plataforma-filmes-movies/filmes`, execute:

```powershell
.\mvnw.cmd clean test
```

No Linux/macOS: `sh mvnw clean test`. Com Maven instalado: `mvn clean test`.
A primeira execução precisa de acesso à internet para baixar o Maven e as dependências.

Abra **[tests/index.html](tests/index.html)** para consultar a cobertura por pacote,
classe, linha e decisão. Também são gerados `tests/jacoco.xml` e `tests/jacoco.csv`.
Os resultados individuais ficam em `target/surefire-reports/`. Os relatórios são
gerados localmente e ignorados pelo Git.

O padrão segue `projeto-software-2026-2-pagamento`: JUnit, Mockito nos serviços,
`@SpringBootTest` com MockMvc nos controllers e JaCoCo 0.8.15, gerando relatórios
na fase `test` em `tests/`. Nenhuma classe foi excluída da medição e não foi definido
um limite mínimo de cobertura no Maven, assim como no projeto de referência.

## Cenários cobertos

- Cadastro e atualização com normalização dos textos e conversão completa para DTO.
- Listagem com filtro ausente, vazio e por prefixo, incluindo remoção de espaços.
- Consultas do repository com ordenação, comparação sem diferenciar caixa e exclusão dos inativos.
- Atualização da avaliação e validação dos limites 0 e 10.
- Substituição e remoção das plataformas de streaming.
- URLs HTTP/HTTPS e rejeição de protocolo ou sintaxe inválidos.
- Redirecionamento 302 com `Location` e erro de plataforma indisponível.
- Exclusão lógica: registro preservado no banco e oculto nas operações públicas.
- Campos obrigatórios e valores inválidos, com respostas estruturadas 400 e 404.
- Inicialização do contexto Spring, persistência do elenco e das plataformas.

O perfil `test` usa H2 em memória no modo PostgreSQL, com criação e descarte do
schema. Controllers são testados com o serviço e o repository reais. O banco é
limpo entre os testes HTTP; os testes de repository usam rollback. Não requer
Docker nem PostgreSQL externo. H2 não reproduz todas as particularidades do
PostgreSQL; a compatibilidade específica com o banco de produção exige testes nesse banco.
