# Frontend

React 19 + TypeScript + Vite + Tailwind 4. Roteamento com React Router 7.

## Direção visual

Tema claro e arejado, inspirado em landing de conferência:

| Token | Valor | Uso |
| --- | --- | --- |
| `--color-canvas` | `#f8f6ff` | fundo da página |
| `--color-surface` | `#ffffff` | cards |
| `--color-ink` / `--color-ink-soft` | `#17103a` / `#5b5576` | texto principal e secundário |
| `--color-brand-600/700` | `#7c3aed` / `#6d28d9` | CTA e links |
| `--color-pastel-*` | lilás, rosa, azul | gradiente do hero |

Componentes base ficam em `src/index.css` (`.card`, `.btn-primary`, `.btn-secondary`, `.field`,
`.chip`, `.hero-gradient`), então as telas não repetem classes utilitárias longas.

Regras de acessibilidade aplicadas: texto principal com contraste ≥ 7:1, foco sempre visível
(`:focus-visible` com anel lilás), alvos de toque com `min-height: 44px`, `prefers-reduced-motion`
desligando transições e `aria-label` nos botões de quantidade.

## Rotas

| Rota | Tela | Protegida |
| --- | --- | --- |
| `/` | descoberta de eventos (busca, filtros, paginação) | não |
| `/login` | entrar ou criar conta (participante ou organizador) | não |
| `/events/:id` | detalhe, tipos de ingresso e compra | compra exige login |
| `/tickets` | meus ingressos com QR Code | sim |
| `/organizer` | painel: métricas, novo evento e publicação | sim (organizador/ADMIN) |
| `/checkin` | portaria: valida o código do ingresso | sim (organizador/ADMIN) |

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

## Painel do organizador

`/organizer` mostra as métricas de `GET /api/organizer/dashboard`, lista os eventos de
`GET /api/events/mine` e permite:

- criar evento **junto com o primeiro tipo de ingresso** — um evento sem ingresso não vende, então
  o formulário cria os dois em sequência;
- publicar um rascunho (`PATCH /api/events/{id}/publish`).

O evento nasce `DRAFT` e só aparece na busca depois de publicado.

## Portaria

`/checkin` valida o código do ingresso em `POST /api/checkins/validate` e traduz os códigos de erro
para linguagem de operação:

| `code` | Mensagem na tela |
| --- | --- |
| `INVALID_QR_TOKEN` | ingresso não encontrado |
| `TICKET_ALREADY_USED` | ingresso já utilizado |
| `FORBIDDEN` | você não gerencia este evento |

A entrada é digitada/colada. Leitor de câmera exigiria biblioteca de scan e permissão de mídia —
fica como evolução; o QR exibido em `/tickets` pode ser lido por qualquer app de leitura e o
código colado aqui.

## O que não existe

- testes de frontend (Vitest + Testing Library) — a suíte cobre backend;
- edição de evento e gestão de múltiplos tipos de ingresso pela UI (existem na API);
- leitura de QR pela câmera na portaria;
- refresh automático de token: expirou, o `401` derruba a sessão e o usuário reloga.
