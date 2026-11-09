# API REST

Base: `/api`. JSON em tudo. Datas em ISO-8601 UTC (`2027-03-20T12:00:00Z`).

## Convenções

- **Autenticação:** `Authorization: Bearer <accessToken>` (a partir da etapa 2).
- **Identidade:** o backend deriva o usuário do token. Nenhum endpoint aceita
  `userId`/`organizerId` no corpo.
- **Erros:** envelope único, sem stack trace.

```json
{
  "timestamp": "2026-11-08T20:00:00Z",
  "status": 409,
  "error": "CONFLICT",
  "code": "TICKET_SOLD_OUT",
  "message": "Nao ha ingressos disponiveis para este tipo.",
  "path": "/api/orders",
  "traceId": "3f1c9b7e"
}
```

- **Paginação:** `?page=0&size=20&sort=startDate,asc`; resposta com `content`, `page`,
  `size`, `totalElements`.
- **Status:** `201` em criação com `Location`, `204` em remoção, `400` validação, `401`
  sem token, `403` sem permissão, `404` inexistente, `409` conflito de regra de negócio,
  `422` regra semântica (ex.: pedido expirado).

## Implementado

| Método | Endpoint | Acesso | Resposta |
| --- | --- | --- | --- |
| GET | `/api/health` | público | `{"status":"UP","service":"eventhub","timestamp":"..."}` |
| POST | `/api/auth/register` | público | `201` + tokens e usuário |
| POST | `/api/auth/login` | público | `200` + tokens e usuário |
| POST | `/api/auth/refresh` | público (exige refresh token) | `200` + novo par de tokens |
| POST | `/api/auth/logout` | autenticado | `204` |
| PATCH | `/api/auth/password` | autenticado | `204` |
| GET | `/api/users/me` | autenticado | perfil |
| PATCH | `/api/users/me` | autenticado | perfil atualizado |
| GET | `/api/events` | público | página de eventos publicados |
| GET | `/api/events/{id}` | público | detalhe (rascunho só para dono/ADMIN) |
| GET | `/api/events/mine` | `ORGANIZER`/`ADMIN` | página com os eventos do organizador |
| POST | `/api/events` | `ORGANIZER`/`ADMIN` | `201` + `Location` |
| PUT | `/api/events/{id}` | dono/`ADMIN` | evento atualizado |
| PATCH | `/api/events/{id}/publish` | dono/`ADMIN` | publica rascunho |
| PATCH | `/api/events/{id}/unpublish` | dono/`ADMIN` | volta para rascunho |
| DELETE | `/api/events/{id}` | dono/`ADMIN` | `204` — cancela, não apaga |

### Eventos

Listagem publica só devolve `PUBLISHED`. Filtros combináveis, todos opcionais:

```http
GET /api/events?category=TECHNOLOGY&city=Campina%20Grande&q=java&from=2027-01-01T00:00:00Z&page=0&size=9
```

```json
{
  "content": [
    {
      "id": "9f2c...",
      "name": "Java Developers Conference",
      "category": "TECHNOLOGY",
      "city": "Campina Grande",
      "startDate": "2027-03-20T09:00:00Z",
      "status": "PUBLISHED",
      "organizerId": "1a2b..."
    }
  ],
  "page": 0,
  "size": 9,
  "totalElements": 1,
  "totalPages": 1
}
```

Regras:

- `city` compara sem diferenciar maiúsculas; `q` busca no nome; `from`/`to` filtram a data de
  início.
- Ordenação padrão: `startDate asc` na busca, `createdAt desc` em `/mine`.
- O organizador vem **do token**, nunca do corpo.
- `ATTENDEE` cria evento → `403`.
- Organizador edita evento de terceiro → `403`.
- Rascunho de terceiro em `GET /{id}` → `404` (não confirma existência).
- Publicar um evento que não está em `DRAFT` → `409 EVENT_NOT_PUBLISHABLE`.
- Editar cancelado/finalizado → `409 EVENT_NOT_EDITABLE`.
- `DELETE` cancela (`CANCELLED`) e é idempotente; eventos finalizados não podem ser cancelados.

### Sessão

O access token é um JWT HS256 de 15 minutos. O refresh token é **opaco** (32 bytes
aleatórios), gravado apenas como SHA-256, válido por 7 dias e de uso único: cada
`/refresh` revoga o token apresentado e devolve outro. Reusar um token revogado falha —
é assim que uma credencial copiada é detectada.

```http
POST /api/auth/login
Content-Type: application/json

{"email":"ana@example.com","password":"senha1234"}
```

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "kM3...",
  "expiresIn": 900,
  "user": { "id": "9f2c...", "name": "Ana", "email": "ana@example.com", "role": "ATTENDEE", "createdAt": "..." }
}
```

Regras aplicadas:

- `register` aceita `role` opcional (`ATTENDEE` padrão, `ORGANIZER` permitido);
  `ADMIN` é recusado com `403` — nunca se auto-promove por payload.
- E-mail duplicado devolve `409` com `code: EMAIL_ALREADY_USED`.
- E-mail inexistente e senha errada devolvem **a mesma** resposta
  (`401 INVALID_CREDENTIALS`), sem revelar quem tem conta.
- Trocar a senha revoga todos os refresh tokens do usuário.
- Sem token ou token inválido em rota protegida: `401 INVALID_TOKEN`.

## Contrato planejado

| Método | Endpoint | Papel | Etapa |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | público | ✅ |
| POST | `/api/auth/login` | público | ✅ |
| POST | `/api/auth/refresh` | público | ✅ |
| POST | `/api/auth/logout` | autenticado | ✅ |
| GET | `/api/users/me` | autenticado | ✅ |
| PATCH | `/api/users/me` | autenticado | ✅ |
| PATCH | `/api/auth/password` | autenticado | ✅ |
| GET | `/api/events` | público | ✅ |
| GET | `/api/events/{id}` | público | ✅ |
| GET | `/api/events/mine` | `ORGANIZER` | ✅ |
| POST | `/api/events` | `ORGANIZER` | ✅ |
| PUT | `/api/events/{id}` | dono | ✅ |
| DELETE | `/api/events/{id}` | dono | ✅ |
| PATCH | `/api/events/{id}/publish` | dono | ✅ |
| PATCH | `/api/events/{id}/unpublish` | dono | ✅ |
| POST | `/api/events/{id}/ticket-types` | dono | 4 |
| GET | `/api/events/{id}/ticket-types` | público | 4 |
| POST | `/api/orders` | `ATTENDEE` | 5 |
| GET | `/api/orders/me` | autenticado | 5 |
| GET | `/api/orders/{id}` | dono | 5 |
| POST | `/api/orders/{id}/payments` | dono | 5 |
| GET | `/api/tickets/me` | autenticado | 6 |
| POST | `/api/checkins/validate` | `ORGANIZER`/`ADMIN` | 6 |
| GET | `/api/organizer/dashboard` | `ORGANIZER` | 7 |

## Exemplo: criar evento

```http
POST /api/events
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{
  "name": "Java Developers Conference",
  "description": "Evento sobre desenvolvimento Java.",
  "category": "TECHNOLOGY",
  "city": "Campina Grande",
  "startDate": "2027-03-20T09:00:00Z",
  "endDate": "2027-03-20T18:00:00Z"
}
```

```http
201 Created
Location: /api/events/9f2c...
```

## Autorização (regras que viram teste)

| Cenário | Esperado |
| --- | --- |
| `ATTENDEE` cria evento | `403` |
| `ORGANIZER` edita evento de outro | `403` (ou `404`, para não revelar existência) |
| Sem token em rota protegida | `401` |
| Token expirado | `401` |

A checagem de propriedade acontece no `Service`, não no controller e nunca no frontend.

## OpenAPI

A especificação é gerada a partir do código (springdoc) na etapa 8 e publicada em
`/swagger-ui.html`. Até lá este documento é o contrato.
