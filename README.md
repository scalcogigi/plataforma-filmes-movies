# Plataforma de Filmes — Movies Service

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
