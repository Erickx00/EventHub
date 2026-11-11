# Convenção de commits — EventHub

Padrão: [Conventional Commits 1.0.0](https://www.conventionalcommits.org/pt-br/v1.0.0/).

```
<tipo>(<escopo>): <descrição>
```

- **Descrição** no imperativo, minúscula, sem ponto final, até ~72 caracteres.
- **Um commit = uma mudança coerente.** Se precisa de "e" na descrição, provavelmente são dois commits.
- O corpo explica **por quê**, não o quê (o diff já mostra o quê).

## Tipos

| Tipo | Uso |
| --- | --- |
| `feat` | Nova funcionalidade para o usuário |
| `fix` | Correção de bug |
| `refactor` | Mudança interna sem alterar comportamento |
| `test` | Testes (adicionar, corrigir, refatorar) |
| `docs` | Documentação |
| `build` | Dependências, build, configuração de projeto |
| `ci` | Pipeline, GitHub Actions |
| `chore` | Manutenção que não muda código de produção |
| `perf` | Melhoria de performance |
| `style` | Formatação, espaços, imports (sem mudança lógica) |

## Escopos

| Escopo | Abrange |
| --- | --- |
| `user` | Cadastro e perfil |
| `auth` | Login, JWT, refresh token |
| `security` | Spring Security, autorização, BCrypt |
| `event` | Módulo de eventos |
| `ticket` | Tipos de ingresso, estoque |
| `order` | Pedidos, reservas |
| `payment` | Simulador de pagamento |
| `checkin` | QR Code e validação de entrada |
| `notification` | E-mails de notificação |
| `dashboard` | Painel do organizador |
| `db` | Migrações Flyway, modelagem |
| `backend` | Configuração geral do backend |
| `frontend` | Configuração geral do frontend |
| `docker` | Dockerfiles e Compose |
| `api` | Contrato/documentação OpenAPI |

Sem escopo quando a mudança é global (`chore: add editorconfig`).

## Regras de ouro

1. Nunca commite direto na `main`. Use `feat/…`, `fix/…`, `chore/…`.
2. `feat` e `fix` entram com teste. Sem teste, é `refactor` ou `chore`.
3. Um commit de migração de banco (`db:`) nunca fica junto com código de aplicação — a ordem do histórico precisa permitir rollback.
4. `BREAKING CHANGE:` no rodapé quando o contrato da API muda de forma incompatível.

## Habilitar o template local

```bash
git config commit.template .gitmessage.txt
```

## Hooks que verificam a convenção

O repositório traz dois hooks em `.githooks/`. Eles são **opt-in** — ativar é um comando:

```bash
git config core.hooksPath .githooks
```

| Hook | Faz |
| --- | --- |
| `pre-commit` | recusa `git add` de `.env`, `.env.local`, chaves privadas (`.pem`, `.p12`, `.p8`, `id_rsa`) |
| `commit-msg` | recusa mensagem fora do padrão `tipo(escopo): descrição` |

`.env.example` e `.env.sample` passam de propósito: são documentação.

Pular uma vez, quando necessário: `git commit --no-verify`.

Em ambiente restrito onde o `sh` do Git não consegue rodar, o hook falha antes da mensagem ser
avaliada — nesse caso não ative o `core.hooksPath`; valide pelo `COMMITS.md`.

## Exemplos por etapa

**Etapa 1 — Fundação**

```
chore: initialize eventhub repository
build(backend): configure spring boot project
build(frontend): initialize react with typescript
build(docker): configure postgres container
db: add initial flyway migration
```

**Etapa 2 — Usuários e autenticação**

```
feat(user): implement user entity and repository
feat(auth): implement user registration
feat(auth): implement jwt authentication
feat(security): configure role-based authorization
test(auth): add authentication service tests
```

**Etapa 3 — Eventos**

```
feat(event): implement event management
feat(event): add event publishing workflow
feat(event): implement event search and filters
feat(frontend): create event discovery page
```

**Etapa 4 — Ingressos e estoque**

```
feat(ticket): implement ticket type management
feat(ticket): add inventory control
fix(ticket): prevent overselling with atomic stock updates
test(ticket): add concurrent reservation tests
```

**Etapa 5 — Pedidos e pagamentos**

```
feat(order): implement order creation
feat(order): implement temporary ticket reservation
feat(payment): add payment simulation
feat(order): implement automatic expiration
test(order): cover order lifecycle
```

**Etapa 6 — QR Code e check-in**

```
feat(ticket): generate digital tickets
feat(ticket): generate unique qr codes
feat(checkin): implement ticket validation
fix(checkin): prevent duplicate ticket validation
```

**Etapas 7 e 8 — Dashboard, testes e publicação**

```
feat(dashboard): implement organizer analytics
test(integration): cover purchase and checkin workflows
docs(api): add openapi documentation
ci: configure github actions
build(docker): containerize full application
docs: add project setup and architecture guide
```

## Verificação antes de abrir PR

```bash
git log --oneline origin/main..HEAD     # histórico legível?
git log --format="%s" | head -20        # todos no padrão?
```
