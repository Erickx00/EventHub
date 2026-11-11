# Frontend

React 19 + TypeScript + Vite + Tailwind 4. Roteamento com React Router 7.

## Rotas

| Rota | Tela | Protegida |
| --- | --- | --- |
| `/` | descoberta de eventos (busca, filtros, paginação) | não |
| `/login` | entrar ou criar conta (participante ou organizador) | não |
| `/events/:id` | detalhe, tipos de ingresso e compra | compra exige login |
| `/tickets` | meus ingressos com QR Code | sim |

## Sessão

`lib/session.ts` guarda `accessToken`, `refreshToken` e o usuário no `localStorage`.

Guardar token no navegador é o compromisso prático de um SPA sem BFF. Mitigações já aplicadas:
access token de 15 min, refresh rotativo de uso único (revogado a cada `/refresh`), e qualquer
`401` limpa a sessão local em `lib/api.ts`.

Alternativa mais segura seria cookie `httpOnly` + `SameSite` com endpoint de refresh no mesmo
domínio, o que exige um BFF — fora do escopo deste projeto.

## Fluxo de compra

1. `GET /api/events/{id}` + `GET /api/events/{id}/ticket-types`
2. usuário escolhe quantidade (limitada por `maxPerOrder` e estoque)
3. `POST /api/orders` — o backend calcula o total
4. `POST /api/orders/{id}/payments` com `outcome: APPROVED`

O passo 4 é o **simulador**. Num gateway real, o passo 3 devolveria um `checkoutUrl` e o
navegador seria redirecionado; o frontend nunca escolheria o resultado do pagamento.

## QR Code

`GET /api/tickets/{id}/qr` exige `Authorization`, então `<img src="...">` não serve: o app busca
o PNG como `Blob` com o header e exibe via `URL.createObjectURL`. As URLs são revogadas no
`cleanup` do efeito para não vazar memória.

## O que não existe

- testes de frontend (Vitest + Testing Library) — a suíte cobre backend;
- painel do organizador na UI (o endpoint existe, a tela não);
- formulário de criação de evento;
- refresh automático de token: expirou, o `401` derruba a sessão e o usuário reloga.
