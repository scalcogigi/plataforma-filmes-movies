# Plataforma de Filmes — Movies Service

## Deploy na AWS

O workflow `.github/workflows/deploy.yml` executa os testes em pull requests e na branch
`main`. Ele exige 80% de cobertura de instruções no JaCoCo antes de publicar a imagem no
Docker Hub e atualizar `movies-service` na AWS por SSH. O contêiner usa a rede Docker
`rede` compartilhada com Users e escuta na porta 8081. O banco PostgreSQL permanece
em um volume próprio e não é publicado na internet.

Antes do primeiro deploy, crie a rede no servidor (`docker network create rede`, se
ela ainda não existir) e suba o PostgreSQL usando `deploy/postgres.compose.yml`.
Defina `DB_USER` e `DB_PASSWORD` no ambiente do servidor ou em um arquivo de ambiente
que permaneça apenas no servidor; execute `docker compose -f postgres.compose.yml up -d --wait`
na pasta que contém esse arquivo. O banco deve estar acessível pelo nome
`postgres-filmes` na rede `rede` e usar `filmesdb` como nome do banco.

Cadastre estes GitHub Actions secrets **no repositório Movies**:

| Secret | Uso |
| --- | --- |
| `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` | Publicar a imagem |
| `AWS_HOST`, `AWS_USER`, `AWS_PRIVATE_KEY` | Acessar o servidor por SSH |
| `DB_USER`, `DB_PASSWORD` | Mesmos valores do PostgreSQL no servidor |
| `TMDB_API_KEY` | Importar títulos e trailers |
| `AUTH0_ISSUER_URI`, `AUTH0_AUDIENCE`, `AUTH0_JWK_SET_URI` | Validar tokens Auth0 |
| `FRONTEND_ORIGIN` | Origem HTTPS do front publicado na Vercel |

Em `AUTH0_AUDIENCE`, use `https://api.plataforma-filmes`. O issuer deve terminar com
`/`. A imagem publicada só é atualizada após um push em `main` que passe nos testes
e no limite de cobertura. O fluxo de deploy verifica `GET /filmes` na porta 8081;
se o serviço não responder, o job falha. Configure o DNS, HTTPS e as URLs públicas
do front separadamente antes de disponibilizar a aplicação aos usuários.

### Primeiro deploy na instância AWS

1. Crie uma instância EC2 Linux com Docker e Docker Compose, IP público estável (Elastic IP) e acesso SSH. No Security Group, permita TCP 80/443 para o público e SSH 22 apenas do endereço de administração. Não abra 5432, 27017, 8080 ou 8081 ao público. Reserve dois nomes DNS (por exemplo, `movies.seudominio.com` e `users.seudominio.com`) com registros A apontando para o IP da instância.
2. Copie os arquivos de `deploy/` para uma pasta na instância. Crie a rede `docker network create rede`. Defina `DB_USER` e `DB_PASSWORD` apenas na instância e execute `docker compose -f postgres.compose.yml up -d --wait` nessa pasta. O volume `postgres-filmes-data` preserva os filmes em reinícios.
3. Configure e execute o banco MongoDB e o serviço Users na mesma rede `rede`, conforme o repositório Users. O contêiner da API deve chamar-se `user-service`; o banco deve estar disponível como `mongo-users`.
4. Defina `MOVIES_DOMAIN=movies.seudominio.com` e `USERS_DOMAIN=users.seudominio.com` na instância e execute `docker compose -f gateway.compose.yml up -d` na pasta `deploy/`. O Caddy encaminha HTTPS para `movies-service:8081` e `user-service:8080` e renova os certificados automaticamente quando DNS e portas 80/443 estiverem corretos.
5. Cadastre os secrets da tabela acima no GitHub do repositório Movies. Use `AUTH0_ISSUER_URI=https://dev-xiu6cdl8suiyoyc6.us.auth0.com/`, `AUTH0_AUDIENCE=https://api.plataforma-filmes` e `AUTH0_JWK_SET_URI=https://dev-xiu6cdl8suiyoyc6.us.auth0.com/.well-known/jwks.json`. `FRONTEND_ORIGIN` deve ser o domínio HTTPS final da Vercel, sem `/` no fim. Após um push na branch `main`, acompanhe Actions; o job publica a imagem e atualiza `movies-service` se testes e cobertura passarem.
6. Confira `https://movies.seudominio.com/filmes` no navegador. A URL `https://users.seudominio.com/users` deve responder `401` sem token, confirmando que HTTPS chega à API protegida. Configure essas duas origens como `MOVIES_API_URL` e `USERS_API_URL` na Vercel e publique o front. Atualize também `FRONTEND_ORIGIN` no repositório Users e execute seu deploy.

O deploy do Movies Service pode ocorrer antes do gateway, mas a Vercel só conseguirá consultar a API quando houver um endereço HTTPS público. O erro atual `Service not found` do Auth0 é uma configuração de audiência no Auth0 e precisa ser resolvido antes de testar login e ações autenticadas.

## Catálogo, TMDB e avaliações

Defina `TMDB_API_KEY` no ambiente do serviço para habilitar a importação. `GET /catalogo/pesquisar?termo=...` consulta primeiro os títulos existentes no PostgreSQL. Quando não encontra, pesquisa filmes e séries na fonte externa e salva até cinco resultados individualmente, incluindo até cinco links de trailers do YouTube. A chave `tipo:tmdbId` e a combinação título/data evitam duplicatas. Para uma carga inicial de até 40 títulos, configure `CATALOG_SEED_COUNT=20` antes da primeira execução com banco vazio; também é possível chamar `POST /catalogo/populares?quantidade=20` com token Auth0.

`GET /filmes/{id}/avaliacoes` lista avaliações publicamente. `PUT /filmes/{id}/avaliacoes/me` grava ou atualiza a nota e o comentário do usuário autenticado (`{ "nota": 8.5, "comentario": "Gostei do filme." }`). Filmes, séries e avaliações são lidos publicamente; cadastro manual, carga de populares e avaliações pessoais exigem autenticação. Configure `FRONTEND_ORIGIN=http://localhost:5173` caso use outra origem no front.

O feed da comunidade usa `GET /comunidade/publicacoes` (público), `POST /comunidade/publicacoes`, `POST /comunidade/publicacoes/{id}/comentarios` e `PUT /comunidade/publicacoes/{id}/curtida` ou `/salvo` (com JWT). Os posts de exemplo são salvos uma vez quando a tabela está vazia; novas interações têm contagens reais no banco.

## Testes automatizados

Na pasta `filmes`, execute `./mvnw.cmd clean test` no Windows ou `sh mvnw clean test`
no Linux/macOS, com JDK 21 ou superior. O relatório de cobertura fica em
`filmes/tests/index.html`. Veja [cenários e configuração dos testes](filmes/TESTES.md).

API REST responsável pelo gerenciamento do catálogo de filmes da plataforma, incluindo dados cinematográficos, elenco, classificação indicativa, avaliação e disponibilidade em serviços de streaming. O serviço foi desenvolvido com Java e Spring Boot, seguindo uma arquitetura em camadas, separação de responsabilidades, validação de entrada, persistência relacional e tratamento centralizado de erros.

> Este repositório contém o serviço de filmes. As funcionalidades relacionadas a usuários, recomendações personalizadas e integração automática com provedores de streaming serão incorporadas em etapas posteriores.

## Visão geral

O Movies Service concentra as operações relacionadas ao catálogo de filmes da plataforma.

Entre suas responsabilidades estão:

- cadastrar filmes;
- consultar filmes ativos;
- buscar filmes por identificador;
- filtrar filmes pelo início do nome;
- atualizar informações cinematográficas;
- atualizar a avaliação de um filme;
- atualizar as plataformas de streaming disponíveis;
- redirecionar o cliente para uma plataforma cadastrada;
- realizar deleção lógica.

A API foi projetada para posteriormente se integrar aos módulos de usuários, avaliações individuais, recomendações e provedores externos de streaming.

## Estado atual

### Funcionalidades concluídas

- [x] Cadastro de filmes
- [x] Consulta de filmes ativos
- [x] Consulta de filme por ID
- [x] Filtro por início do nome
- [x] Atualização completa de filme
- [x] Atualização específica da avaliação
- [x] Atualização das plataformas de streaming
- [x] Redirecionamento para plataforma cadastrada
- [x] Deleção lógica
- [x] Validação dos dados de entrada
- [x] Validação das URLs de streaming
- [x] Tratamento centralizado de exceções
- [x] Persistência com PostgreSQL

### Funcionalidades planejadas

- [ ] Integração com o serviço de usuários
- [ ] Avaliações individuais por usuário
- [ ] Cálculo da média de avaliações
- [ ] Listas de favoritos, assistidos e “quero assistir”
- [ ] Integração automática com provedores de streaming
- [ ] Sistema de recomendação
- [ ] Testes unitários e de integração
- [ ] Conteinerização da aplicação
- [ ] Pipeline de integração e entrega contínuas
- [ ] Deploy automatizado

## Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java 21 | Linguagem e versão-alvo de compilação |
| Spring Boot 4.1.1 | Estrutura principal da aplicação |
| Spring Web MVC | Implementação da API REST |
| Spring Data JPA | Abstração da camada de persistência |
| Hibernate ORM | Mapeamento objeto-relacional |
| Jakarta Validation | Validação declarativa dos DTOs |
| PostgreSQL 17 | Banco de dados relacional |
| Lombok | Redução de código repetitivo |
| Maven Wrapper | Build reproduzível do projeto |
| Docker | Execução local do PostgreSQL |

## Modelo de domínio

A entidade `Filme` possui os seguintes atributos:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` | Identificador gerado pelo banco |
| `nome` | `String` | Nome do filme |
| `diretor` | `String` | Diretor responsável |
| `casting` | `List<String>` | Relação de atores e atrizes |
| `lancamento` | `LocalDate` | Data de lançamento |
| `plataformasDisponiveis` | `Map<String, String>` | Plataforma associada à sua URL |
| `classificacaoIndicativa` | `Integer` | Classificação etária |
| `duracao` | `Integer` | Duração em minutos |
| `genero` | `String` | Gênero cinematográfico |
| `avaliacao` | `BigDecimal` | Avaliação entre 0 e 10 |
| `ativo` | `Boolean` | Estado utilizado na deleção lógica |

O Hibernate cria três tabelas principais:

```text
filmes
filme_casting
filme_streamings
```

As tabelas `filme_casting` e `filme_streamings` possuem chaves estrangeiras para `filmes`.

## Requisitos

Para executar o projeto localmente, são necessários:

- Java 21 ou superior;
- Docker;
- Maven Wrapper incluído no repositório;
- porta `8081` disponível para a API;
- porta `5433` disponível para o PostgreSQL.

Verifique o Java:

```bash
java --version
```

No Windows PowerShell:

```powershell
.\mvnw.cmd --version
```

Em Linux ou macOS:

```bash
./mvnw --version
```

## Configuração

A aplicação aceita configurações por variáveis de ambiente.

| Variável | Valor padrão | Finalidade |
|---|---|---|
| `SERVER_PORT` | `8081` | Porta HTTP da aplicação |
| `DB_HOST` | `localhost` | Endereço do PostgreSQL |
| `DB_PORT` | `5433` | Porta local do PostgreSQL |
| `DB_NAME` | `filmesdb` | Nome do banco |
| `DB_USER` | `usuario` | Usuário do banco |
| `DB_PASSWORD` | `senha` | Senha do banco |

Configuração de desenvolvimento presente em `application.properties`:

```properties
spring.application.name=filmes

server.port=${SERVER_PORT:8081}

spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:filmesdb}
spring.datasource.username=${DB_USER:usuario}
spring.datasource.password=${DB_PASSWORD:senha}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Os valores padrão são destinados exclusivamente ao desenvolvimento local. Credenciais reais não devem ser armazenadas no repositório.

## Execução local

### 1. Acessar o módulo Spring

No Windows PowerShell:

```powershell
cd .\filmes
```

### 2. Iniciar o PostgreSQL

```powershell
docker run -d `
  --name filmes-db `
  --restart unless-stopped `
  -e POSTGRES_DB=filmesdb `
  -e POSTGRES_USER=usuario `
  -e POSTGRES_PASSWORD=senha `
  -p 5433:5432 `
  -v filmes-postgres-data:/var/lib/postgresql/data `
  postgres:17
```

Em Linux ou macOS:

```bash
docker run -d \
  --name filmes-db \
  --restart unless-stopped \
  -e POSTGRES_DB=filmesdb \
  -e POSTGRES_USER=usuario \
  -e POSTGRES_PASSWORD=senha \
  -p 5433:5432 \
  -v filmes-postgres-data:/var/lib/postgresql/data \
  postgres:17
```

Verifique o container:

```bash
docker ps
```

Aguarde a mensagem:

```text
database system is ready to accept connections
```

Ela pode ser consultada com:

```bash
docker logs filmes-db
```

### 3. Compilar o projeto

No Windows:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Em Linux ou macOS:

```bash
./mvnw clean package -DskipTests
```

### 4. Executar a aplicação

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Em Linux ou macOS:

```bash
./mvnw spring-boot:run
```

A API estará disponível em:

```text
http://localhost:8081
```

O início correto é confirmado pelas mensagens:

```text
Tomcat started on port 8081
Started FilmesApplication
```

## API REST

URL base:

```text
http://localhost:8081/filmes
```

### Endpoints

| Método | Endpoint | Descrição | Resposta esperada |
|---|---|---|---|
| `POST` | `/filmes` | Cadastra um filme | `201 Created` |
| `GET` | `/filmes` | Lista os filmes ativos | `200 OK` |
| `GET` | `/filmes?nome={prefixo}` | Filtra pelo início do nome | `200 OK` |
| `GET` | `/filmes/{id}` | Busca um filme ativo | `200 OK` |
| `PUT` | `/filmes/{id}` | Atualiza integralmente um filme | `200 OK` |
| `PATCH` | `/filmes/{id}/avaliacao` | Atualiza a avaliação | `200 OK` |
| `PUT` | `/filmes/{id}/streamings` | Substitui as plataformas disponíveis | `200 OK` |
| `GET` | `/filmes/{id}/redirect?plataforma={nome}` | Redireciona para o streaming | `302 Found` |
| `DELETE` | `/filmes/{id}` | Realiza a deleção lógica | `204 No Content` |

## Exemplos de uso

### Cadastrar um filme

```http
POST /filmes
Content-Type: application/json
```

```json
{
  "nome": "Interestelar",
  "diretor": "Christopher Nolan",
  "casting": [
    "Matthew McConaughey",
    "Anne Hathaway",
    "Jessica Chastain"
  ],
  "lancamento": "2014-11-06",
  "plataformasDisponiveis": {
    "Prime Video": "https://www.primevideo.com/",
    "Max": "https://www.max.com/"
  },
  "classificacaoIndicativa": 10,
  "duracao": 169,
  "genero": "Ficção científica",
  "avaliacao": 8.7
}
```

Exemplo de resposta:

```json
{
  "id": 1,
  "nome": "Interestelar",
  "diretor": "Christopher Nolan",
  "casting": [
    "Matthew McConaughey",
    "Anne Hathaway",
    "Jessica Chastain"
  ],
  "lancamento": "2014-11-06",
  "plataformasDisponiveis": {
    "Prime Video": "https://www.primevideo.com/",
    "Max": "https://www.max.com/"
  },
  "classificacaoIndicativa": 10,
  "duracao": 169,
  "genero": "Ficção científica",
  "avaliacao": 8.7
}
```

### Listar filmes

```http
GET /filmes
```

### Filtrar por nome

O filtro utiliza a estratégia `startsWith`, ignorando diferenças entre letras maiúsculas e minúsculas.

```http
GET /filmes?nome=Int
```

Esse exemplo pode retornar `Interestelar`, mas não um filme no qual `Int` apareça somente no meio do nome.

### Buscar por ID

```http
GET /filmes/1
```

### Atualizar integralmente um filme

```http
PUT /filmes/1
Content-Type: application/json
```

O corpo segue o mesmo contrato utilizado no cadastro.

### Atualizar a avaliação

```http
PATCH /filmes/1/avaliacao
Content-Type: application/json
```

```json
{
  "avaliacao": 9.2
}
```

A avaliação aceita valores entre `0.0` e `10.0`.

> Nesta versão, o campo representa uma avaliação diretamente associada ao filme. Quando a integração com usuários for implementada, cada avaliação será vinculada a um usuário e a nota do filme passará a representar uma média calculada.

### Atualizar plataformas

```http
PUT /filmes/1/streamings
Content-Type: application/json
```

```json
{
  "plataformasDisponiveis": {
    "Netflix": "https://www.netflix.com/",
    "Prime Video": "https://www.primevideo.com/"
  }
}
```

Essa operação substitui o conjunto atual de plataformas do filme.

### Redirecionar para uma plataforma

```http
GET /filmes/1/redirect?plataforma=Netflix
```

Resposta:

```http
HTTP/1.1 302 Found
Location: https://www.netflix.com/
```

O nome da plataforma é comparado sem diferenciação entre letras maiúsculas e minúsculas.

### Excluir logicamente

```http
DELETE /filmes/1
```

Resposta:

```http
HTTP/1.1 204 No Content
```

O registro permanece armazenado no banco, mas deixa de aparecer nas consultas públicas.

## Tratamento de erros

A API possui tratamento centralizado de exceções.

### Filme não encontrado

Status:

```http
404 Not Found
```

Exemplo:

```json
{
  "timestamp": "2026-09-28T16:30:00",
  "status": 404,
  "erro": "Recurso não encontrado",
  "mensagem": "Filme com id 99 não encontrado",
  "caminho": "/filmes/99",
  "campos": {}
}
```

### Erro de validação

Status:

```http
400 Bad Request
```

Exemplo:

```json
{
  "timestamp": "2026-09-28T16:30:00",
  "status": 400,
  "erro": "Erro de validação",
  "mensagem": "Existem campos inválidos na requisição",
  "caminho": "/filmes",
  "campos": {
    "nome": "O nome é obrigatório",
    "duracao": "A duração deve ser maior que zero"
  }
}
```

### Plataforma indisponível

Status:

```http
400 Bad Request
```

Exemplo:

```json
{
  "timestamp": "2026-09-28T16:30:00",
  "status": 400,
  "erro": "Requisição inválida",
  "mensagem": "O filme não está disponível na plataforma informada",
  "caminho": "/filmes/1/redirect",
  "campos": {}
}
```

## Persistência e deleção lógica

A remoção de filmes utiliza deleção lógica.

Quando um filme é excluído, o atributo:

```text
ativo
```

é alterado de `true` para `false`.

O registro permanece no PostgreSQL para preservar consistência e histórico, mas os métodos públicos do repository consultam somente registros ativos.

As consultas principais utilizam:

```text
findByAtivoTrueOrderByNomeAsc
findByAtivoTrueAndNomeStartingWithIgnoreCaseOrderByNomeAsc
findByIdAndAtivoTrue
```
