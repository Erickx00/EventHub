# Banco de dados

PostgreSQL 17. Schema versionado exclusivamente por Flyway (`backend/src/main/resources/db/migration`).

## Convenções

- `snake_case`, tabelas no plural.
- PK `UUID` com `gen_random_uuid()` (extensão `pgcrypto`).
- `created_at`/`updated_at` em `timestamptz`, default `now()`.
- Enums como `VARCHAR` + `CHECK` (migrar para tipo nativo só se precisar de ordem).
- Índice em toda FK usada em filtro e em toda coluna de busca (`email`).

## Estado atual (V1)

### users

| Coluna | Tipo | Notas |
| --- | --- | --- |
| `id` | uuid | PK, `gen_random_uuid()` |
| `name` | varchar(120) | obrigatório |
| `email` | varchar(180) | único, sempre minúsculo |
| `password_hash` | varchar(100) | BCrypt (60 chars, margem para upgrade) |
| `role` | varchar(20) | `ATTENDEE` \| `ORGANIZER` \| `ADMIN` (CHECK) |
| `created_at` / `updated_at` | timestamptz | default `now()` |

## Modelo alvo

```
users 1---N events          (organizer)
users 1---N orders          (buyer)
events 1---N ticket_types
orders 1---N order_items
ticket_types 1---N order_items
orders 1---N payments
order_items 1---N tickets
tickets 1---1 check_ins
```

| Tabela | Papel |
| --- | --- |
| `events` | nome, descrição, categoria, cidade, início/fim, status |
| `ticket_types` | preço, `total_quantity`, `available_quantity`, janela de venda |
| `orders` | total, status, `expires_at` |
| `order_items` | quantidade + `unit_price` congelado |
| `payments` | tentativas, status, método, `paid_at` |
| `tickets` | `qr_token` único, status, `issued_at` |
| `check_ins` | validação, `validated_by`, `checked_at` |

## Estados

```java
enum EventStatus   { DRAFT, PUBLISHED, CANCELLED, FINISHED }
enum OrderStatus   { PENDING, PAID, CANCELLED, EXPIRED }
enum PaymentStatus { PENDING, APPROVED, REFUSED, REFUNDED }
enum TicketStatus  { VALID, USED, CANCELLED }
```

Transições válidas (o resto é erro de domínio, não de banco):

| De | Para | Gatilho |
| --- | --- | --- |
| `Order.PENDING` | `PAID` | pagamento aprovado |
| `Order.PENDING` | `EXPIRED` | job de expiração |
| `Order.PENDING` | `CANCELLED` | usuário cancela |
| `Order.PAID` | `CANCELLED` | evento cancelado (com estorno) |
| `Ticket.VALID` | `USED` | check-in único |
| `Ticket.VALID` | `CANCELLED` | cancelamento do pedido |

## Concorrência

O controle de estoque não pode ser `SELECT` seguido de `UPDATE` em código Java: entre os
dois, outra transação vende o mesmo ingresso. Duas opções válidas:

```sql
-- 1. update condicional atomico (preferido: sem lock explicito)
UPDATE ticket_types
SET available_quantity = available_quantity - :qty
WHERE id = :id AND available_quantity >= :qty;
-- linha afetada = 0  =>  sem estoque
```

```java
// 2. lock pessimista na leitura
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<TicketType> findByIdForUpdate(UUID id);
```

O `ORDER_ITEM` só é criado depois que o estoque foi reservado com sucesso, na mesma
transação. Expiração devolve estoque com outro update condicional e muda o pedido para
`EXPIRED` — nunca apaga o pedido.

## Regras de migração

1. Migração aplicada é imutável. Correção = nova versão (`V2__...sql`).
2. Um arquivo por mudança coerente; nome no padrão `V<n>__<snake_case>.sql`.
3. `DROP`/rename só com o código antigo já removido do deploy (expand-contract).
4. Toda migração roda em dev e em CI antes de chegar na `main`.
