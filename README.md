# Gestão de compras (Projeto J)

Sistema de gestão de compras com suporte a **identidade/acesso** e no módulo de **compras**, composto por API REST (Backend) e aplicação desktop (FrontEnd).

## Visão geral

O projeto permite administrar usuários, perfis e permissões, e executar o fluxo completo de compras — da requisição ao recebimento — com auditoria de operações e indicadores no dashboard.

**Fluxo típico de compras**

```text
Produtos / Fornecedores
        ↓
Req. compras → Aprovar RC → Solicitar cotação (RFQ)
        ↓
Propostas → Mapa comparativo → Pedido de compra → Recebimento
```

## Estrutura do repositório

```text
Projeto J/
├── Backend/     # API REST (Spring Boot)
└── FrontEnd/    # App desktop (JavaFX)
```

## Stack tecnológica

| Camada | Tecnologias |
|--------|-------------|
| Linguagem | Java 21 |
| Build | Maven |
| Backend | Spring Boot 3.3.5, Spring Data JPA, Validation, Flyway |
| Banco | PostgreSQL |
| API docs | springdoc / OpenAPI (Swagger) |
| FrontEnd | JavaFX 21 (FXML + CSS), Jackson, HttpClient |
| Testes | JUnit 5, Mockito, Testcontainers |

## Módulos

### Cadastros (Sistema)
- **Usuários** — autenticação e vínculo a perfil
- **Perfis** — papéis e permissões
- **Permissões** — módulo × ação (VIEW, CREATE, UPDATE, DELETE…)
- **Módulos** — catálogo das áreas do sistema

### Compras — Cadastros
- **Produtos** — itens de compra (com fornecedor principal)
- **Fornecedores** — cadastro de fornecedores e moeda padrão

### Compras — Operacional
- **Req. compras** — solicitação de necessidade (rascunho → submissão)
- **Aprovar RC** — aprovação, rejeição ou devolução
- **Solicitar cotação (RFQ)** — cotação com fornecedores e orientações
- **Propostas** — registro das ofertas recebidas
- **Mapa Comparativo** — adjudicação
- **Pedidos de Compra** — emissão, envio e follow-up
- **Recebimentos** — entrada de materiais

### Sistema
- **Dashboard Compras** — KPIs e gráficos
- **Log de API** — auditoria de operações
- **Conectividade** — status da API

## Pré-requisitos

- JDK 21+
- Maven 3.9+
- PostgreSQL 14+ (banco `projeto_j`)

## Como executar

### 1. Banco de dados

Crie o banco PostgreSQL:

```sql
CREATE DATABASE projeto_j;
```

Configuração padrão (perfil `dev`):

- URL: `jdbc:postgresql://localhost:5432/projeto_j`
- Usuário/senha: `postgres` / `postgres` (ou variáveis `DB_USERNAME` / `DB_PASSWORD`)

As migrations Flyway são aplicadas automaticamente na subida do Backend.

### 2. Backend

```bash
cd Backend
mvn spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/api/v1/health`

### 3. FrontEnd

```bash
cd FrontEnd
mvn javafx:run
```

O cliente aponta para `http://localhost:8080/api/v1` (`application.properties`).

## Autenticação

Login via headers `X-Auth-Username` / `X-Auth-Password`. A API emite uma chave de sessão (API Key) válida por 1 hora, usada nas requisições seguintes.

O menu do FrontEnd respeita as permissões do perfil do usuário autenticado.

## Auditoria

Registros do módulo de compras mantêm **criado por** e **atualizado por** (ID e nome), preenchidos automaticamente pela API — não editáveis pela interface.

## Licença

Uso interno / proprietário — ajuste conforme a política da organização.
