# Deploy sem cartão

Três serviços gratuitos, nenhum exige cartão:

```
navegador
   │  https://eventhub-xxxx.vercel.app
   ▼
Vercel (SPA estática)  ──fetch──►  https://eventhub-api-xxxx.onrender.com
                                        │  JDBC + sslmode=require
                                        ▼
                                  Neon (PostgreSQL gratuito)
```

| Serviço | Papel | Plano | Cartão? |
| --- | --- | --- | --- |
| [Neon](https://neon.tech) | PostgreSQL | Free | não |
| [Render](https://render.com) | API (Docker) | Web free | não |
| [Vercel](https://vercel.com) | frontend | Hobby | não |

O repositório já tem `render.yaml` (blueprint da API) e `frontend/vercel.json` (build + rewrite de
SPA). O banco não fica no Render de propósito: lá o PostgreSQL gratuito **expira em ~30 dias** e o
pago exige cartão — num portfólio isso significa link quebrado no mês seguinte.

## Passo 1 — Banco no Neon

1. Crie conta em <https://neon.tech> (login pelo GitHub, sem cartão).
2. **Create project**: nome `eventhub`, região mais próxima (`AWS us-east-1` ou `sa-east-1` se
   disponível), versão do Postgres 17.
3. No painel, abra **Connect** e copie os campos do connection string:

```
postgresql://neondb_owner:SENHA@ep-cool-name-123456.us-east-1.aws.neon.tech/neondb?sslmode=require
                └─ DB_USER ─┘ └DB_PASSWORD┘ └────────── DB_HOST ──────────────┘ └DB_NAME┘
```

| Variável | Valor |
| --- | --- |
| `DB_HOST` | `ep-cool-name-123456.us-east-1.aws.neon.tech` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `neondb` |
| `DB_USER` | `neondb_owner` |
| `DB_PASSWORD` | a senha gerada (clique para revelar) |

O `sslmode=require` já vai fixo no `render.yaml` via `DB_PARAMS`.

## Passo 2 — API no Render

1. Conta em <https://render.com> (login pelo GitHub, sem cartão).
2. **New → Blueprint** → selecione o repositório `EventHub`.
3. O Render lê o `render.yaml` e cria só o `eventhub-api`. Ele vai pedir os valores com
   `sync: false` — cole os cinco `DB_*` do passo 1.
4. Confirme o plano **Free** do web service e aplique.
5. Espere o build (~5 min). O health check bate em `/api/health`; quando ficar verde, copie a URL:
   `https://eventhub-api-xxxx.onrender.com`.

`JWT_SECRET` é gerado pelo Render — não invente nem reaproveite o de desenvolvimento.

## Passo 3 — Frontend na Vercel

1. Conta em <https://vercel.com> (login pelo GitHub).
2. **Add New → Project** → importe o mesmo repositório.
3. **Root Directory**: `frontend` (o framework Vite é detectado).
4. Environment Variable, em Production:
   `VITE_API_URL = https://eventhub-api-xxxx.onrender.com`
5. Deploy e copie o domínio: `https://eventhub-xxxx.vercel.app`.

O `VITE_API_URL` entra no bundle em **build time**: trocar depois exige novo deploy.

## Passo 4 — fechar o CORS

Render → `eventhub-api` → **Environment**:

```
CORS_ALLOWED_ORIGINS = https://eventhub-xxxx.vercel.app
```

Salve (redeploy automático). Sem isso o navegador bloqueia toda chamada com erro de CORS — a API
responde, mas o browser descarta a resposta.

## Passo 5 — smoke test

```bash
curl https://eventhub-api-xxxx.onrender.com/api/health
# {"status":"UP","service":"eventhub","timestamp":"..."}

open https://eventhub-api-xxxx.onrender.com/swagger-ui.html
```

Depois, no site: criar conta como organizador → criar evento → publicar → outro navegador como
participante → comprar → ver o QR → validar em **Portaria**.

## Variáveis de ambiente

| Variável | Onde | Obrigatória | Padrão |
| --- | --- | --- | --- |
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` | Render | sim | — |
| `DB_PARAMS` | Render | recomendada | — (`?sslmode=require` no blueprint) |
| `JWT_SECRET` | Render | sim (≥32 bytes) | valor de dev |
| `CORS_ALLOWED_ORIGINS` | Render | sim em produção | `http://localhost:5173` |
| `ORDER_PAYMENT_TTL` | Render | não | `PT15M` |
| `ORDER_EXPIRY_SCAN_MS` | Render | não | `60000` |
| `EVENT_FINISH_SCAN_MS` | Render | não | `300000` |
| `VITE_API_URL` | Vercel | sim | vazio (usa proxy do Vite em dev) |

## Limites dos planos gratuitos

| Serviço | Limite prático | Efeito na demo |
| --- | --- | --- |
| Render web free | dorme após ~15 min sem tráfego | primeira requisição leva ~30 s |
| Neon free | storage e compute-horas limitados; suspende o compute por inatividade | primeira query após ociosidade leva ~1 s |
| Vercel Hobby | uso pessoal, bandwidth limitada | irrelevante para demo |

Confira os números atuais nos sites deles — mudam com o tempo. Para eliminar o cold start do
Render, troque `plan: free` por `starter` (~US$ 7/mês) no `render.yaml`.

## Alternativa: banco no Render (pago)

Se preferir tudo no Render, adicione ao `render.yaml`:

```yaml
databases:
  - name: eventhub-db
    databaseName: eventhub
    user: eventhub
    plan: basic-256mb      # plano pago; o free expira em ~30 dias
    postgresMajorVersion: "17"
```

E troque os `DB_*` com `sync: false` por `fromDatabase: { name: eventhub-db, property: host }`
(um para cada: `host`, `port`, `database`, `user`, `password`). Exige cartão cadastrado.

## Troubleshooting

| Sintoma | Causa provável |
| --- | --- |
| `SSL connection is required` | falta `DB_PARAMS=?sslmode=require` |
| `blocked by CORS policy` | `CORS_ALLOWED_ORIGINS` sem o domínio exato (sem barra no fim) |
| `404` ao recarregar `/events/123` | falta o rewrite de SPA (`frontend/vercel.json`) |
| Health check falha e o serviço reinicia | `DB_*` errado ou banco do Neon suspenso |
| `Flyway ... failed` | schema antigo no banco; crie um banco novo no Neon |
| Login sempre `401` | `JWT_SECRET` mudou entre deploys (invalida todos os tokens) |
| API responde mas o frontend mostra erro de rede | `VITE_API_URL` errado ou sem `https://` |
