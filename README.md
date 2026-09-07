# Gestão operacional (Projeto J)

Sistema corporativo de gestão operacional com foco em **identidade/acesso** e no módulo de **compras**, composto por API REST (Backend) e aplicação desktop (FrontEnd).

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

O sistema é **stateless**. Não há cookie de sessão no servidor.

1. O FrontEnd envia `POST /api/v1/auth/login` com usuário e senha nos headers:
   - `X-Auth-Username`
   - `X-Auth-Password`
2. O Backend valida usuário ativo, senha (BCrypt) e perfil (`role`) ativo.
3. A API emite uma **API Key** de sessão (1 hora), gravada em `auth_sessions`.
4. Um novo login do mesmo usuário **revoga** a chave anterior e gera outra.
5. As demais chamadas autenticadas enviam a mesma chave em:
   - `Authorization: Bearer <apiKey>` e/ou
   - `X-Api-Key: <apiKey>`

### Endpoints públicos (não exigem API Key)

- `POST /api/v1/auth/login`
- `GET /api/v1/health`
- Swagger / OpenAPI (`/swagger-ui.html`, `/v3/api-docs/**`)

Qualquer outro `/api/v1/**` exige chave válida. Sem chave, inválida ou expirada, a API responde **401** (`AUTH_REQUIRED` ou `AUTH_SESSION_EXPIRED`).

## Permissionamento

O modelo é **RBAC** (controle de acesso baseado em perfil): usuário → **1 perfil (role)** → conjunto de **permissões**. Cada permissão é a combinação **módulo × ação**.

### Modelo de dados (Backend)

```text
users ──► roles ──► role_permissions ──► permissions
                                              │
                         modules ◄────────────┤
                         actions ◄────────────┘
```

| Tabela | Função |
|--------|--------|
| `modules` | Áreas do sistema (`USERS`, `PRODUCTS`, `PURCHASE_REQUISITIONS`…) |
| `actions` | Operações (`VIEW`, `CREATE`, `UPDATE`, `DELETE`, `SUBMIT`, `APPROVE`…) |
| `permissions` | Par único `module_id` + `action_id` |
| `roles` | Perfil (ex.: `ADMIN`) |
| `role_permissions` | Quais permissões o perfil possui |
| `user_permissions` | Tabela extra por usuário (modelo previsto; o login atual usa o perfil) |

O código da permissão, usado em toda a aplicação, é:

```text
MODULO:ACAO
```

Exemplos: `USERS:VIEW`, `PRODUCTS:CREATE`, `PURCHASE_REQUISITIONS:UPDATE`, `API_LOGS:VIEW`.

### Como o Backend monta as permissões no login

Em `AuthRestController`, após autenticar o usuário:

1. Carrega o `role` vinculado.
2. Lê `role.permissions`.
3. Converte cada permissão para `module.getCode() + ":" + action.getCode()`.
4. Devolve a lista no `LoginResponse.permissions`, junto com `userId`, `roleId`, `roleName`, `apiKey` e `expiresAt`.

As permissões **não são gravadas na sessão** (`auth_sessions` guarda só usuário + API Key). Elas são enviadas **uma vez**, no login, para o cliente.

### O que o Backend efetivamente bloqueia hoje

| Camada | Comportamento |
|--------|----------------|
| `ApiKeyAuthenticationFilter` | Exige API Key válida em `/api/v1/**` (exceto login/health/docs) |
| Spring Security | `authenticated()` — não usa `hasAuthority` / `hasRole` |
| Controllers | Usam `AuthenticatedUser.requireUserId()` para auditoria e regras de negócio |
| Checagem `MODULO:ACAO` | **Ainda não é aplicada no servidor** |

Ou seja: o Backend autentica *quem* é o usuário, mas **ainda não recusa** uma operação só porque o perfil não tem `PRODUCTS:CREATE`, por exemplo.

Exceção de regra de negócio (não é permissão de módulo): em **Aprovar RC**, só o aprovador designado (`approverUserId`) pode editar comentários ou decidir.

> A lista `permissions` do login existe para o FrontEnd. Qualquer cliente com uma API Key válida consegue, hoje, chamar os endpoints REST. A aplicação desktop é o ponto que esconde menus e botões.

### Como o FrontEnd aplica o permissionamento

No login, `LoginUseCase` grava a lista no `SessionContext`. Daí em diante:

| Método | Permissão exigida | Uso |
|--------|-------------------|-----|
| `canView(modulo)` | `MODULO:VIEW` | Exibir item de menu e abrir a tela |
| `canCreate(modulo)` | `MODULO:CREATE` | Botão Novo / criar |
| `canUpdate(modulo)` | `MODULO:UPDATE` | Botão Editar / ações de alteração |
| `canDelete(modulo)` | `MODULO:DELETE` | Botão Excluir |

**Menu (`ShellController`)**

- Cada botão é ligado a um código de módulo (`USERS`, `PRODUCTS`, `RFQS`…).
- Sem `MODULO:VIEW`, o botão some (`visible` / `managed` = false).
- Grupos do menu (Cadastros, Compras, Sistema) só aparecem se pelo menos um item interno for visível.

**Telas de CRUD (`AbstractPurchasingListController` e cadastros de identidade)**

- `CREATE` controla se o botão de inclusão aparece.
- `UPDATE` / `DELETE` habilitam Editar / Excluir após selecionar um registro.
- Abrir a tela de novo exige `VIEW` (`openIfCan`).

**Logout** limpa a API Key no cliente e o `SessionContext` (incluindo as permissões).

### Módulos usados no menu × permissão

| Tela / grupo | Código do módulo | Permissão para ver |
|--------------|------------------|--------------------|
| Usuários | `USERS` | `USERS:VIEW` |
| Perfis | `ROLES` | `ROLES:VIEW` |
| Permissões | `PERMISSIONS` | `PERMISSIONS:VIEW` |
| Módulos | `MODULES` | `MODULES:VIEW` |
| Produtos | `PRODUCTS` | `PRODUCTS:VIEW` |
| Fornecedores | `SUPPLIERS` | `SUPPLIERS:VIEW` |
| Req. compras | `PURCHASE_REQUISITIONS` | `PURCHASE_REQUISITIONS:VIEW` |
| Aprovar RC | `REQUISITION_APPROVALS` | `REQUISITION_APPROVALS:VIEW` |
| Solicitar cotação (RFQ) | `RFQS` | `RFQS:VIEW` |
| Propostas | `SUPPLIER_QUOTES` | `SUPPLIER_QUOTES:VIEW` |
| Mapa Comparativo | `QUOTE_AWARDS` | `QUOTE_AWARDS:VIEW` |
| Pedidos de Compra | `PURCHASE_ORDERS` | `PURCHASE_ORDERS:VIEW` |
| Recebimentos | `GOODS_RECEIPTS` | `GOODS_RECEIPTS:VIEW` |
| Log de API | `API_LOGS` | `API_LOGS:VIEW` |
| Conectividade | `SYSTEM` | `SYSTEM:VIEW` |

O perfil **ADMIN** recebe, nas migrations, as permissões dos módulos de identidade e de compras.

### Fluxo resumido

```text
Login (headers usuário/senha)
        ↓
Backend valida credenciais + perfil
        ↓
Emite API Key (1h) + lista MODULO:ACAO do perfil
        ↓
FrontEnd guarda SessionContext
        ↓
Menu e botões respeitam VIEW / CREATE / UPDATE / DELETE
        ↓
Chamadas seguintes: mesma API Key
        ↓
Backend: só valida a chave (autenticação), não o MODULO:ACAO
```

## Auditoria

Registros do módulo de compras mantêm **criado por** e **atualizado por** (ID e nome), preenchidos automaticamente pela API — não editáveis pela interface.

## Licença

Uso interno / proprietário — ajuste conforme a política da organização.
