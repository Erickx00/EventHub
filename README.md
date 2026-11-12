# EventHub

Plataforma de eventos, venda de ingressos e controle de entrada — monólito modular em **Java 21 + Spring Boot 4** com frontend em **React 19**.

[![CI](https://github.com/Erickx00/EventHub/actions/workflows/ci.yml/badge.svg)](https://github.com/Erickx00/EventHub/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![License MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Demo:** [event-hub-front.vercel.app](https://event-hub-front.vercel.app) · **API:** documentação interativa em `/swagger-ui.html` da API publicada (URL no painel do Render)

> A API dorme no plano gratuito: a primeira requisição depois de alguns minutos leva ~30 s.
> Abra o link antes de mostrar para alguém.

---

## O problema

Vender ingresso é fácil até dois clientes comprarem o **último** ao mesmo tempo. A partir daí o
projeto deixa de ser CRUD e vira consistência: estoque, dinheiro, duplicidade de entrada. Este
repositório foi construído em torno desse ponto.

## Quem usa

| Perfil | Pode |
| --- | --- |
| **Participante** | descobrir eventos, comprar ingressos, apresentar o QR Code |
| **Organizador** | criar e publicar eventos, definir ingressos, acompanhar vendas e validar entradas |
| **Admin** | gerenciar eventos de qualquer organizador |

## Fluxo principal

```
organizador                     participante
     │                               │
 cria evento (DRAFT)                 │
       └── publica ──────────────► busca e abre o evento
                                     │
                                     ├── escolhe ingressos
                                     ├── POST /api/orders      (reserva estoque, 15 min)
                                     ├── POST /payments        (simulado)
                                     └── recebe ingresso + QR
                                                   │
                                    portaria valida o código
                                                   │
                              check-in único ──► painel atualizado
```

## Telas

| Rota | Tela |
| --- | --- |
| `/` | descoberta: busca, filtro por cidade/categoria, paginação |
| `/login` | entrar ou criar conta (participante ou organizador) |
| `/events/:id` | detalhe, tipos de ingresso e compra |
| `/tickets` | meus ingressos com QR Code |
| `/organizer` | métricas, criação de evento, publicação |
| `/checkin` | portaria: valida o código do ingresso |

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Backend | Spring Boot 4.1 · Spring Security · Spring Data JPA |
| Autenticação | JWT HS256 (15 min) + refresh token opaco rotativo |
| Banco | PostgreSQL 17 · Flyway (schema é fonte de verdade) |
| QR Code | ZXing |
| Documentação | springdoc/OpenAPI 3 (`/swagger-ui.html`) |
| Frontend | React 19 · TypeScript 7 · Vite 8 · Tailwind 4 · React Router 7 |
| Infra | Docker · Docker Compose · Render · Neon · Vercel |
| CI | GitHub Actions (backend + PostgreSQL de serviço, frontend) |
| Testes | JUnit 5 · Mockito · AssertJ · MockMvc |

## Decisões que sustentam o sistema

Nem toda decisão aparece num CRUD. Estas são as que mudam o comportamento sob carga:

**Estoque sem oversell.** A baixa é um `UPDATE` condicional, nunca leitura seguida de escrita:

```sql
UPDATE ticket_types
   SET available_quantity = available_quantity - :qty
 WHERE id = :id AND available_quantity >= :qty;
```

Zero linhas afetadas = estoque acabou entre a leitura e a escrita → `409 TICKET_SOLD_OUT`.

**Check-in que não duplica.** Mesmo padrão no ingresso (`status = 'VALID'`), reforçado por
`UNIQUE (ticket_id)` em `check_ins`: se a validação atômica for removida um dia, o banco ainda
recusa o segundo check-in.

**Pagamento não cobra duas vezes.** O pedido é lido com `PESSIMISTIC_WRITE`; dois cliques
simultâneos serializam e o segundo encontra `PAID`.

**Pedido vencido não é mutado dentro do pagamento.** Marcar `EXPIRED` e lançar exceção na mesma
transação faria o rollback desfazer a própria expiração — quem transiciona é o job.

**Cancelar evento tem cascata.** `EventCancelled` é publicado na mesma transação: pedidos
pendentes voltam estoque, pedidos pagos são estornados e todos os ingressos são invalidados. É
evento de domínio, não chamada direta, justamente para não criar ciclo de beans.

**Total nunca vem do cliente.** O corpo traz evento, tipo e quantidade; preço e total saem do
banco. `order_items.unit_price` congela o preço da compra.

**Erro tem contrato.** Envelope único com `code` estável (`TICKET_SOLD_OUT`, `ORDER_EXPIRED`…) e
`X-Request-Id` correlacionando resposta e log.

**Schema é código.** Flyway versiona tudo; Hibernate roda com `ddl-auto=validate` e falha no
startup se entidade e banco divergirem.

## Arquitetura

```
com.eventhub
├── auth        registro, login, refresh, logout, senha
├── user        perfil
├── event       CRUD, publicação, busca, encerramento por data
├── ticket      tipos de ingresso, estoque atômico, ingresso digital
├── order       pedido, itens, expiração, cancelamento em cascata
├── payment     gateway simulado atrás de interface
├── checkin     validação única de entrada
├── dashboard   agregações do organizador
├── security    JWT, filtro, CORS, regras de rota
└── shared      erros, paginação, request id, config
```

Cada módulo tem controller/DTO/service/repository próprios; módulos conversam por serviço ou por
evento de domínio, nunca por repositório alheio. Detalhes e trade-offs em
[docs/architecture.md](docs/architecture.md).

## Rodando localmente

```bash
cp .env.example .env
docker compose up -d                 # PostgreSQL em localhost:5432
cd backend && ./mvnw spring-boot:run # Flyway aplica as migrações
cd frontend && npm install && npm run dev
```

Site em <http://localhost:5173>, API em <http://localhost:8080>, Swagger em
<http://localhost:8080/swagger-ui.html>. Passo a passo, portas e troubleshooting:
[docs/setup.md](docs/setup.md).

## Testes

```bash
cd backend && ./mvnw test     # 105 testes, sem banco
cd frontend && npm run typecheck
```

O CI sobe um PostgreSQL de serviço e roda também o `SchemaValidationTest`, que aplica as seis
migrações e valida o mapeamento JPA contra o banco real. Sem banco disponível, ele é pulado em vez
de falhar.

## Deploy

Neon (PostgreSQL) + Render (API em Docker) + Vercel (frontend) — **sem cartão**. O blueprint está
em [render.yaml](render.yaml), a configuração da Vercel em [frontend/vercel.json](frontend/vercel.json)
e o guia completo em [docs/deploy.md](docs/deploy.md).

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [docs/setup.md](docs/setup.md) | rodar local, variáveis, troubleshooting |
| [docs/architecture.md](docs/architecture.md) | camadas, módulos, decisões e dívidas conhecidas |
| [docs/database.md](docs/database.md) | modelo, estados e regras de migração |
| [docs/inventory.md](docs/inventory.md) | estoque e concorrência |
| [docs/orders.md](docs/orders.md) | ciclo do pedido, pagamento, expiração, cancelamento |
| [docs/tickets.md](docs/tickets.md) | ingresso digital e check-in |
| [docs/frontend.md](docs/frontend.md) | rotas, sessão no cliente, fluxo de compra |
| [docs/deploy.md](docs/deploy.md) | deploy, variáveis e limites dos planos gratuitos |
| [docs/api.md](docs/api.md) | contrato REST e códigos de erro |
| [COMMITS.md](COMMITS.md) | convenção de commits (com hooks opcionais) |

## Limites conhecidos

Documentados em [docs/architecture.md](docs/architecture.md#o-que-ainda-não-existe-e-por-quê):
pagamento é simulado, não há reembolso parcial, testes de frontend ainda não existem e a portaria
aceita código digitado (sem leitura por câmera).

## Licença

[MIT](LICENSE).
