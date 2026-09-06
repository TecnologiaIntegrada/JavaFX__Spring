# Projeto J — FrontEnd

Aplicacao desktop JavaFX para administracao corporativa de usuarios, perfis, modulos e permissoes.

Comunicacao exclusiva com o Backend via REST API — nunca acessa PostgreSQL diretamente.

## Arquitetura

Inspirada em MVVM + Hexagonal:

- **UI (FXML/Controller)** — interacao visual
- **ViewModel** — estado da tela (JavaFX Properties)
- **Use Case** — orquestracao
- **Port (Gateway)** — abstracao da API
- **Adapter (REST)** — HTTP via `ApiClient`

## Pre-requisitos

- Java 21 LTS
- Maven 3.9+
- Backend em execucao em `http://localhost:8080`

## Configuracao

Arquivo `src/main/resources/application.properties`:

```properties
api.base-url=http://localhost:8080/api/v1
```

Para overrides locais, crie `application-local.properties` (ignorado pelo Git).

## Executar

```bash
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
mvn javafx:run
```

Na tela inicial, clique em **Verificar conexao** para validar a comunicacao com o Backend.

## Testes

```bash
mvn test
```

## Estrutura (Fase 1)

```
src/main/java/com/projetoj/frontend/
├── MainApplication.java
└── shared/
    ├── application/     # Use cases
    ├── config/          # Configuracao externa
    ├── di/              # Composition Root
    ├── http/            # ApiClient
    └── ui/              # Controllers JavaFX
```

## Fases de implementacao

1. **Foundation** — ApiClient, health check, DI container
2. **Domain** — modelos internos, ports
3. **REST Adapters** — User, Role, Permission, Module
4. **UI** — Shell, listas, formularios, matriz de permissoes
5. **EventBus** — Observer para refresh desacoplado
6. **Authorization UX** — controle de botoes por permissao
