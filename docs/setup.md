# Rodando o EventHub localmente

## Pré-requisitos

| Ferramenta | Versão mínima | Como conferir |
| --- | --- | --- |
| JDK | 21 | `java -version` |
| Maven | 3.9 | `mvn -v` |
| Node.js | 20 | `node --version` |
| Docker + Compose | 24 | `docker compose version` |

## 1. Variáveis de ambiente

```bash
cp .env.example .env
```

O `.env` é lido pelo Docker Compose e documenta as variáveis do backend e do frontend.
Nunca commite esse arquivo.

## 2. Banco de dados

```bash
docker compose up -d
docker compose ps          # postgres deve ficar "healthy" em ~10s
```

PostgreSQL em `localhost:5432`, banco `eventhub`, usuário `eventhub`.

## 3. Backend

```bash
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"   # PowerShell: $env:JWT_SECRET = ...
./mvnw spring-boot:run
```

Sem `JWT_SECRET` o backend usa um valor de desenvolvimento (>= 32 bytes) — serve para
rodar local, **nunca** para produção.

O Flyway aplica as migrações na subida. API em <http://localhost:8080>.

```bash
curl http://localhost:8080/api/health
# {"status":"UP","service":"eventhub","timestamp":"2026-11-08T20:00:00Z"}
```

Testes:

```bash
./mvnw test
```

## 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Interface em <http://localhost:5173>. O Vite faz proxy de `/api` para `localhost:8080`,
então não há CORS em desenvolvimento.

## Portas

| Serviço | Porta |
| --- | --- |
| Frontend (Vite) | 5173 |
| Backend (Spring) | 8080 |
| PostgreSQL | 5432 |

## Scripts úteis

| Onde | Comando | Faz |
| --- | --- | --- |
| `backend/` | `./mvnw test` | testes |
| `backend/` | `./mvnw spring-boot:run` | sobe a API |
| `frontend/` | `npm run dev` | servidor de desenvolvimento |
| `frontend/` | `npm run build` | build de produção |
| `frontend/` | `npm run typecheck` | só o TypeScript |

## Troubleshooting

**`Port 5432 is already in use`** — outro PostgreSQL local está rodando. Troque
`POSTGRES_PORT` no `.env` e ajuste `DB_PORT` para o mesmo valor.

**`Flyway ... checksum mismatch`** — alguém editou uma migração já aplicada. Nunca edite
`V*.sql` aplicado; crie a próxima versão. Para recomeçar do zero em local:

```bash
docker compose down -v && docker compose up -d
```

**`Connection refused` no backend** — o container ainda não está `healthy`. Espere o
healthcheck ou veja `docker compose logs postgres`.

**`Schema-validation: missing table`** — o Flyway não rodou. Confirme
`spring.flyway.enabled=true` e se `backend/src/main/resources/db/migration/` tem o `V1`.

**402 / proxy do frontend** — sem o backend, o banner mostra "API offline"; é o
comportamento esperado, não um bug.

## Build de produção

```bash
cd backend  && ./mvnw -DskipTests package      # gera target/*.jar
cd frontend && npm run build                   # gera dist/
```

Os Dockerfiles da raiz fazem o build completo:

```bash
docker compose --profile full up --build   # postgres + backend + frontend
```

Sem o `--profile full`, o Compose sobe apenas o PostgreSQL — o fluxo normal de
desenvolvimento, com backend e frontend rodando na máquina.
