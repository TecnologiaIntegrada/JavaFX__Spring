# Projeto J — Backend

Backend corporativo em Java 21 + Spring Boot 3 para gerenciamento de usuarios, perfis, modulos e permissoes.

## Arquitetura

Organizacao modular por dominio com Clean Architecture / Hexagonal:

- **Domain** — regras de negocio puras (sem Spring/JPA)
- **Application** — use cases, ports, DTOs
- **Adapter** — REST (inbound), JPA (outbound)

Modulos: `identity/user`, `identity/role`, `identity/permission`, `identity/authorization`, `shared`.

## Pre-requisitos

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 14+ local

## Banco de dados

Criar o banco:

```sql
CREATE DATABASE projeto_j;
```

Variaveis de ambiente:

| Variavel     | Descricao              | Padrao dev |
|--------------|------------------------|------------|
| DB_USERNAME  | Usuario PostgreSQL     | postgres   |
| DB_PASSWORD  | Senha PostgreSQL       | postgres   |

Nunca versionar credenciais reais. Use `application-local.yml` (ignorado pelo Git) para overrides locais.

## Executar

```bash
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
set DB_USERNAME=postgres
set DB_PASSWORD=sua_senha

mvn spring-boot:run
```

Perfil de teste (Testcontainers):

```bash
mvn test -Dspring.profiles.active=test
```

## Endpoints (Fase 1)

| Metodo | URL              | Descricao        |
|--------|------------------|------------------|
| GET    | /api/v1/health   | Status da API    |

## Swagger

- UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

## Flyway

Migrations em `src/main/resources/db/migration/`.

Hibernate configurado com `ddl-auto: validate` — schema gerenciado exclusivamente pelo Flyway.

## Testes

```bash
mvn test
```

## Fases de implementacao

1. **Foundation** — health endpoint, Flyway baseline, estrutura de pacotes
2. **Domain** — entidades, value objects, authorization strategy
3. **Persistence** — JPA, adapters, seed
4. **API** — REST completo, validacao, exception handler
5. **JavaFX** — UI desktop
6. **Security** — JWT, autorizacao por permissao
