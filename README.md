# EventHub

Plataforma de gerenciamento de eventos, venda de ingressos e controle de entrada.

> Status: Sprint 1 concluída — fundação do projeto (backend, frontend, banco e ambiente local).

## O que é

Organizadores criam eventos, definem tipos de ingresso e acompanham vendas. Participantes
descobrem eventos, compram ingressos e apresentam um QR Code na entrada, validado uma única vez.

| Perfil | Faz |
| --- | --- |
| `ATTENDEE` | Descobre eventos, compra ingressos, consulta suas inscrições |
| `ORGANIZER` | Cria eventos, gerencia ingressos, acompanha vendas, faz check-in |
| `ADMIN` | Gerencia usuários, eventos e a plataforma |

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Backend | Spring Boot 4.1 |
| Segurança | Spring Security + JWT |
| Persistência | Spring Data JPA |
| Banco | PostgreSQL 17 |
| Migrações | Flyway |
| Frontend | React 19 + TypeScript |
| Estilo | Tailwind CSS 4 |
| Build frontend | Vite 8 |
| Docs da API | Swagger / OpenAPI |
| Infra | Docker + Docker Compose |
| CI | GitHub Actions |

Arquitetura: **monólito modular**. Cada módulo de negócio é um pacote com suas próprias
camadas; nada de microsserviços nesta fase.

## Como executar

```bash
cp .env.example .env
docker compose up -d          # PostgreSQL em localhost:5432
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

Detalhes, portas e variáveis: [docs/setup.md](docs/setup.md).

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [docs/setup.md](docs/setup.md) | Como rodar localmente, pré-requisitos, troubleshooting |
| [docs/architecture.md](docs/architecture.md) | Camadas, módulos, decisões |
| [docs/database.md](docs/database.md) | Modelo de dados e estados |
| [docs/inventory.md](docs/inventory.md) | Controle de estoque e concorrência |
| [docs/orders.md](docs/orders.md) | Ciclo de vida do pedido, pagamento e expiração |
| [docs/api.md](docs/api.md) | Contrato REST |
| [docs/AGENTS-AND-SKILLS.md](docs/AGENTS-AND-SKILLS.md) | Quais agents/skills do ECC usar em cada etapa |
| [COMMITS.md](COMMITS.md) | Convenção de commits |

## Roadmap

| Etapa | Escopo | Estado |
| --- | --- | --- |
| 1 | Fundação (Spring Boot, React, Postgres, Flyway, health) | ✅ |
| 2 | Usuários e autenticação (JWT, BCrypt, papéis) | ✅ |
| 3 | Gerenciamento de eventos (CRUD, publicação, filtros, descoberta) | ✅ |
| 4 | Ingressos e estoque (baixa atômica, limite por pedido) | ✅ |
| 5 | Pedidos e pagamentos (reserva temporária, expiração) | ✅ |
| 6 | QR Code e check-in | ⬜ |
| 7 | Dashboard e testes | ⬜ |
| 8 | Documentação, CI e deploy | ⬜ |
