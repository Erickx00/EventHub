# Deploy

Arquitetura: **API + PostgreSQL no Render**, **frontend na Vercel**. Custo: o banco usa o plano
pago `basic-256mb` (o gratuito do Render expira em ~30 dias) e o web service fica no plano
gratuito, com o cold start descrito no fim.

```
navegador
   │  https://eventhub-xxxx.vercel.app
   ▼
Vercel (SPA estática)  ──fetch──►  https://eventhub-api.onrender.com
                                        │
                                        ▼
                                  PostgreSQL gerenciado (Render)
```

## O que só você pode fazer

Criar contas, autorizar o GitHub e clicar nos botões. O repositório já tem tudo que o Render e a
Vercel precisam: `render.yaml` (blueprint) e `frontend/vercel.json`.

## Passo 1 — API no Render

1. Crie conta em <https://render.com> e conecte o GitHub.
2. **New → Blueprint** → escolha o repositório `EventHub`.
3. O Render lê o `render.yaml`, cria `eventhub-db` (PostgreSQL 17) e `eventhub-api` (Docker).
4. Espere o primeiro build (~5 min; compila o Maven dentro do Docker).
5. O health check usa `/api/health`. Quando ficar verde, copie a URL:
   `https://eventhub-api-xxxx.onrender.com`.

O que o blueprint já configura sozinho:

| Variável | Origem |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | banco criado pelo blueprint |
| `JWT_SECRET` | gerado pelo Render (`generateValue`) |
| `CORS_ALLOWED_ORIGINS` | placeholder — você ajusta no passo 3 |

A porta vem de `PORT`, injetada pelo Render; o `application.yml` já lê
`server.port: ${SERVER_PORT:${PORT:8080}}`.

## Passo 2 — Frontend na Vercel

1. Crie conta em <https://vercel.com> e importe o mesmo repositório.
2. **Root Directory**: `frontend`.
3. Framework: Vite (detectado). O `vercel.json` já define build e o rewrite de SPA
   (sem ele, `/events/123` dá 404 ao recarregar).
4. Variável de ambiente, em **Production**:
   `VITE_API_URL = https://eventhub-api-xxxx.onrender.com`
5. Deploy. Copie o domínio: `https://eventhub-xxxx.vercel.app`.

O `VITE_API_URL` entra no bundle em **build time**; trocar depois exige novo deploy.

## Passo 3 — fechar o CORS

No Render → `eventhub-api` → Environment, troque:

```
CORS_ALLOWED_ORIGINS = https://eventhub-xxxx.vercel.app
```

Salve (o Render redeploya). Sem isso o navegador bloqueia toda chamada com erro de CORS — o
backend responde, mas o browser descarta a resposta.

## Passo 4 — smoke test

```bash
# API de pé e banco migrado
curl https://eventhub-api-xxxx.onrender.com/api/health
# {"status":"UP","service":"eventhub","timestamp":"..."}

# documentação
open https://eventhub-api-xxxx.onrender.com/swagger-ui.html
```

Depois, no site: criar conta como organizador → criar evento → publicar → abrir outro navegador
como participante → comprar → ver o QR Code.

## Variáveis de ambiente

| Variável | Onde | Obrigatória | Padrão |
| --- | --- | --- | --- |
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` | Render | sim | — |
| `JWT_SECRET` | Render | sim (≥32 bytes) | valor de dev |
| `CORS_ALLOWED_ORIGINS` | Render | sim em produção | `http://localhost:5173` |
| `ORDER_PAYMENT_TTL` | Render | não | `PT15M` |
| `ORDER_EXPIRY_SCAN_MS` | Render | não | `60000` |
| `EVENT_FINISH_SCAN_MS` | Render | não | `300000` |
| `VITE_API_URL` | Vercel | sim | vazio (usa proxy do Vite em dev) |

## Planos e custo

| Recurso | Plano | Consequência |
| --- | --- | --- |
| PostgreSQL | `basic-256mb` (pago) | não expira; storage e conexões compatíveis com o projeto |
| Web service | `free` | dorme após ~15 min sem tráfego, com cold start de ~30 s |
| Frontend (Vercel) | Hobby | estático, sem custo |

O banco foi para plano pago de propósito: o PostgreSQL gratuito do Render é apagado após ~30 dias,
e um portfólio não pode ter o link quebrado no mês seguinte. Se quiser eliminar também o cold
start, troque `plan: free` do `eventhub-api` por `starter` no `render.yaml`.

Ressalva do plano gratuito restante:

- **Web service dorme** após ~15 min sem tráfego; a primeira requisição leva ~30 s. Para demo em
  entrevista, abra o site 1 minuto antes.
- Build do backend no Render é lento porque recompila o Maven a cada deploy; o `Dockerfile` já
  separa `dependency:go-offline` para aproveitar cache.

## Troubleshooting

| Sintoma | Causa provável |
| --- | --- |
| `blocked by CORS policy` | `CORS_ALLOWED_ORIGINS` sem o domínio exato (sem barra no fim) |
| `404` ao recarregar `/events/123` | falta o rewrite de SPA (`frontend/vercel.json`) |
| Health check falha e o app reinicia | banco não pronto ou `DB_*` ausente |
| `Flyway ... failed` no log | banco com schema antigo; recrie o banco no primeiro deploy |
| Login responde 401 sempre | `JWT_SECRET` mudou entre deploys (invalida tokens) ou relógio do container |
| API responde mas frontend vê erro de rede | `VITE_API_URL` errado ou faltando `https://` |
