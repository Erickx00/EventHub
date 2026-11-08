# Agents e skills do ECC para o EventHub

O que está instalado neste ambiente (ECC v2.2.3): **314 skills** em `.dsh/skills/`,
**71 agents** e **98 commands** em `vendor/ECC/`.

Duas verdades antes da tabela:

1. **Skills funcionam nativamente.** São carregadas sob demanda e o corpo entra no
   contexto só quando você pede.
2. **Agents não são auto-dispatchados** por este harness. Cada agent é um `agent.md` com
   persona + processo. O harness tem subagents nativos: o caminho é usar o agent como
   *prompt* de um subagent. Commands (slash commands do Claude Code) não existem aqui —
   o equivalente é carregar a skill correspondente.

## O elenco mínimo (não carregue os 71)

Cinco agents cobrem 90% do projeto. Os outros são para linguagens/domínios que o EventHub
não usa.

| Agent | Quando chamar | Por que este |
| --- | --- | --- |
| `planner` | antes de cada etapa | quebra o escopo em passos com critério de aceite |
| `tdd-guide` | ao implementar qualquer regra de negócio | força test-first, que é o diferencial do projeto |
| `java-reviewer` | depois de cada módulo backend | detecta o que compila mas não deveria existir |
| `security-reviewer` | etapas 2 e 6 (auth, QR, check-in) | o projeto vive de autorização |
| `database-reviewer` | etapa 4 (estoque/concorrência) e qualquer migração | revisa índice, lock e plano de query |

Segunda linha, sob demanda: `commit-reviewer` (mensagem de commit), `silent-failure-hunter`
(erros engolidos), `pr-test-analyzer` (cobertura real do PR), `react-reviewer` +
`typescript-reviewer` (frontend), `java-build-resolver` / `react-build-resolver` (quando o
build quebra), `code-explorer` (entender código alheio), `doc-updater` (docs).

**Não use:** `cpp-reviewer`, `dart-build-resolver`, `go-reviewer`, `rust-*`, `swift-*`,
`django-*`, `fastapi-reviewer`, `php-reviewer`, `ruby-reviewer`, `healthcare-*`,
`network-*`, `homelab-*`, `gan-*`, `seo-specialist` (não é o foco).

## Skills por etapa

| Etapa | Skills que valem a leitura | Para quê |
| --- | --- | --- |
| 1 — Fundação | `springboot-patterns`, `vite-patterns`, `docker-patterns` | estrutura de pacotes, config de Vite, Compose sem armadilha |
| 2 — Auth | `springboot-security`, `security-review`, `api-design` | JWT, BCrypt, 401/403, contrato de erro |
| 3 — Eventos | `api-design`, `postgres-patterns`, `react-patterns` | filtros, paginação, índice, formulário |
| 4 — Ingressos | `database-migrations`, `springboot-tdd` | update atômico, teste de concorrência |
| 5 — Pedidos | `springboot-tdd`, `api-design` | máquina de estados, expiração |
| 6 — QR/check-in | `security-review`, `springboot-tdd` | token não adivinhável, check-in idempotente |
| 7 — Dashboard | `react-performance`, `frontend-patterns`, `verification-loop` | gráficos sem re-render, loop de verificação |
| 8 — Publicação | `deployment-patterns`, `docker-patterns`, `github-ops` | CI, imagem, release |

Transversais, use sempre: `git-workflow` (branches), `conventional-commit-review`
(mensagem), `tdd-workflow` (o ciclo vermelho-verde-refatora), `change-impact` (antes de
mexer em contrato), `code-tour` (explicar o módulo no README/vídeo).

O ECC também tem a família `orch-*` (`orch-add-feature`, `orch-fix-defect`,
`orch-change-feature`, `orch-refine-code`, `orch-build-mvp`): são pipelines
pesquisa → plano → TDD → review → commit. Boas quando a etapa é grande; excessivas para um
endpoint.

## Loop por tarefa

```
1. planner            -> passos + critério de aceite
2. skill tdd-workflow -> escrever o teste que falha
3. implementar        -> menor código que faz o teste passar
4. java-reviewer      -> revisão (subagent, persona do agent)
5. security-reviewer  -> só se tocou auth/permissão/dado sensível
6. commit-reviewer    -> mensagem no padrão de COMMITS.md
7. git commit         -> feat(escopo): ...
```

## Como invocar na prática

- Skill: carregar e seguir as instruções dela (ex.: `tdd-workflow`,
  `springboot-security`).
- Agent: abrir `vendor/ECC/agents/java-reviewer.md`, usar o conteúdo como prompt de um
  subagent com o diff do módulo em questão.
- Command: ignorar. O corpo do `vendor/ECC/commands/<nome>.md` costuma ter a mesma
  instrução de uma skill — use a skill.

## O que evitar

- Carregar 10 skills de uma vez: o catálogo já é grande e o contexto é finito.
- Rodar o pipeline `orch-*` completo para um CRUD de 3 arquivos.
- Aceitar sugestão de agent sem teste que a comprove — o projeto vale pelo que está
  testado, não pelo que foi gerado.
