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

| Método | Endpoint | Resposta |
| --- | --- | --- |
| GET | `/api/health` | `{"status":"UP","service":"eventhub","timestamp":"..."}` |

## Contrato planejado

| Método | Endpoint | Papel | Etapa |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | público | 2 |
| POST | `/api/auth/login` | público | 2 |
| POST | `/api/auth/refresh` | público | 2 |
| POST | `/api/auth/logout` | autenticado | 2 |
| GET | `/api/users/me` | autenticado | 2 |
| PATCH | `/api/users/me` | autenticado | 2 |
| GET | `/api/events` | público | 3 |
| GET | `/api/events/{id}` | público | 3 |
| POST | `/api/events` | `ORGANIZER` | 3 |
| PUT | `/api/events/{id}` | dono | 3 |
| DELETE | `/api/events/{id}` | dono | 3 |
| PATCH | `/api/events/{id}/publish` | dono | 3 |
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
