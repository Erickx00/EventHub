# Auditoria de engenharia — 24/11/2026

Escopo: leitura estática do repositório na `main` remota disponível nesta data. Nenhum serviço foi iniciado, reiniciado ou consultado; nenhum dado de ambiente foi lido; nenhuma migration ou configuração de produção foi executada. A branch `improvement/concurrency` foi criada a partir de `origin/main` atualizado para registrar a auditoria e preparar a primeira melhoria.

## Resumo

O projeto é um monólito modular Java 21/Spring Boot com PostgreSQL/Flyway e SPA React/TypeScript. Os módulos de negócio, DTOs separados das entidades, validações, transações, consultas de estoque condicionais, check-in de uso único, chave estrangeira/constraints e fluxo de erro estruturado formam uma base razoável. A implementação observada é mais completa que partes de `docs/architecture.md`, que ainda descrevem uma UI incompleta e ausência de OpenAPI embora as rotas/telas e springdoc existam.

Risco prioritário observado: pagamento serializa por lock pessimista, mas cancelamento do pedido e expiração automática não usam esse lock. Leituras concorrentes podem sobrescrever transições de estado e devolver estoque de pedido pago. A correção exige código e testes focados e muda comportamento de produção; portanto, precisa de aprovação explícita antes da implementação, conforme instrução do solicitante.

## Achados priorizados

### Alto — corrida entre pagar, cancelar e expirar pedidos

- **Problema/evidência:** `PaymentService.pay` carrega o pedido com `OrderRepository.findByIdForUpdate`, enquanto `OrderService.cancel` usa `requireOwnedOrder`/`findById` e `expireOverdue` carrega a lista sem lock. O listener de cancelamento do evento também lê pedidos pendentes sem lock.
- **Impacto atual:** operações simultâneas podem atuar sobre uma instância desatualizada de `PENDING`. Pagamento pode emitir ingressos e outra operação mudar o pedido para `CANCELLED`/`EXPIRED` e devolver a reserva, permitindo estoque acima do disponível e pedidos pagos sem estado coerente.
- **Solução proposta:** unificar a serialização de transições no agregado do pedido; fazer cancelamento e expiração disputarem o mesmo lock de pagamento. Para expiração em múltiplas instâncias, processar linhas de forma concorrente segura e em lotes, ou usar locks de linha com rechecagem de estado. Não introduzir alteração de schema antes de provar necessidade.
- **Arquivos/módulos:** `backend/src/main/java/com/eventhub/order/service/OrderService.java`, `order/repository/OrderRepository.java`, `order/service/OrderExpirationJob.java`, `order/listener/EventCancelledListener.java`, testes em `backend/src/test/java/com/eventhub/order` e `payment`.
- **Riscos da mudança:** contenção/espera de locks; deadlocks se a ordem de aquisição variar; job segurando locks por muito tempo se processar todos os pedidos vencidos numa única transação.
- **Validação:** testes concorrentes em PostgreSQL real com duas transações para pagar+cancela, pagar+expirar e duas instâncias de expiração; afirmar um único estado final válido, uma emissão no máximo e estoque dentro de `[0,total]`. Executar testes de regressão dos pedidos, pagamentos e cancelamento de eventos. Verificar comportamento e duração do lote sem apontar testes para produção.

### Alto — resultado de pagamento controlado pelo cliente

- **Problema/evidência:** `PaymentRequest` recebido por `PaymentController` inclui o `outcome`, que `PaymentService` passa ao `SimulatedPaymentGateway`. O frontend também oferece escolha de aprovado/recusado.
- **Impacto atual:** qualquer usuário pode aprovar uma compra sem uma confirmação independente do processador. Isso é aceitável apenas como simulação de portfólio; não é seguro para venda real. O README documenta a simulação, mas a aplicação está publicamente implantada.
- **Solução proposta:** antes de habilitar cobrança real, remover resultado do contrato público e derivar aprovação exclusivamente de gateway confiável/webhook autenticado, com idempotência e reconciliação. Para a demo, preservar claramente o modo simulado e não o apresentar como pagamento real.
- **Arquivos/módulos:** `payment/dto/PaymentRequest.java`, `payment/service/PaymentService.java`, `payment/gateway/SimulatedPaymentGateway.java`, `PaymentController`, frontend `src/lib/api.ts` e telas de compra, README/docs de deploy.
- **Riscos:** quebra do contrato da demo e necessidade de configurar credenciais e callbacks se for substituído por gateway real; integrar pagamento externo requer desenho e aprovação próprios.
- **Validação:** provar que requests não conseguem forçar aprovação fora do adaptador de simulação; testes de autorização, idempotência, webhook e replay quando houver gateway real. Não conectar nem cobrar serviço financeiro nesta auditoria.

### Médio — refresh token rotativo pode ser reutilizado em corrida

- **Problema/evidência:** `AuthService.refresh` busca pelo hash, verifica ativo e revoga dentro da transação; a leitura não aparenta usar lock nem update condicional. Requisições simultâneas com o mesmo refresh token podem ambas observar o token ativo antes de revogar.
- **Impacto atual:** uma credencial de uso único pode gerar mais de um par de tokens se duas chamadas concorrentes vencerem a corrida.
- **Solução proposta:** revogação atômica condicional no banco ou lock pessimista da linha, seguida da emissão somente para a chamada que efetivamente revogou.
- **Arquivos/módulos:** `auth/service/AuthService.java`, `auth/repository/RefreshTokenRepository.java`, `auth/entity/RefreshToken.java` e testes de serviço.
- **Riscos:** diferenças de concorrência/transação e conflito com clientes que já repetem refresh automaticamente.
- **Validação:** teste de concorrência com token único assegurando uma única rotação aceita e reuso rejeitado.

### Médio — custo e tempo de execução sem limites operacionais explícitos

- **Problema/evidência:** a expiração carrega todos os pedidos pendentes vencidos numa lista e os processa numa transação; não há rate limit visível para login/registro e o pool Hikari é fixo em 10. CI cobre integração de schema, mas frontend não tem testes automatizados.
- **Impacto atual:** backlog pode gerar transações longas/uso elevado de memória; tentativas de login podem ser abusadas; regressões de interface e fluxos completos não são detectadas automaticamente.
- **Solução proposta:** limitar e particionar o lote de expiração; definir estratégia de rate limit por IP/conta apropriada à infraestrutura; adicionar testes frontend e testes de API/fluxo em ambiente isolado. Medir antes de tunar pool/cache.
- **Arquivos/módulos:** `OrderService`, `OrderExpirationJob`, segurança de autenticação, `frontend/package.json` e `.github/workflows/ci.yml`.
- **Riscos:** limite de rate inadequado pode bloquear usuários atrás de NAT; lote pequeno aumenta atraso de liberação; testes E2E aumentam manutenção/tempo de CI.
- **Validação:** métricas em ambiente local/staging, teste funcional de limites e filas; nunca teste de carga no serviço ativo.

### Médio — credenciais de sessão acessíveis a JavaScript

- **Problema/evidência:** `frontend/src/lib/session.ts` armazena access e refresh tokens em `localStorage`.
- **Impacto atual:** um XSS na origem pode exfiltrar ambos os tokens. O access token é curto e o refresh é rotativo, mas o refresh também está exposto ao JavaScript.
- **Solução proposta:** avaliar BFF/cookie `HttpOnly`, `Secure`, `SameSite` ou mitigação CSP rigorosa junto com estratégia de sessão. Mudança de arquitetura e contrato deve ser planejada, não improvisada nesta auditoria.
- **Arquivos/módulos:** frontend `lib/session.ts`, `lib/api.ts`, auth/backend e cabeçalhos de deploy.
- **Riscos:** cookies exigem defesa CSRF e mudanças no CORS/domínios; BFF adiciona infraestrutura.
- **Validação:** testes de autenticação/CORS/CSRF e inspeção dos cabeçalhos e armazenamento em ambiente local.

### Médio — índice de busca por e-mail declarado sem evidência de normalização no banco

- **Problema/evidência:** `V1__initial_schema.sql` cria índice `users(email)` único, enquanto a normalização em minúsculas ocorre no código (`User.normalizeEmail`). A constraint não impede gravações futuras por outro caminho com caixa diferente.
- **Impacto atual:** unicidade lógica depende de todos os caminhos de escrita normalizarem e-mails; uma escrita divergente pode permitir duplicata em PostgreSQL, dependendo de collation.
- **Solução proposta:** confirmar collation e dados existentes; considerar índice único funcional em `lower(email)` por migration aditiva, após consulta de duplicatas em ambiente controlado e plano expand-and-contract.
- **Arquivos/módulos:** `V1` (imutável), nova migration aditiva, `User`, repositório e testes de schema.
- **Riscos:** migration não pode ser aplicada sem verificar duplicatas e locks em tabela; V1 não deve ser editada.
- **Validação:** teste de migration em banco vazio e cópia sintética representativa, teste de escrita concorrente e verificação de duplicatas antes de qualquer deploy.

### Baixo — documentação de arquitetura e execução está desatualizada

- **Problema/evidência:** `docs/architecture.md` afirma que só há descoberta, que OpenAPI não existe e que módulos futuros ainda nascerão; no código existem login, compra, ingressos, dashboard, check-in e springdoc. O README descreve a interface corretamente.
- **Impacto atual:** onboarding e decisões técnicas podem partir de premissas incorretas.
- **Solução proposta:** reconciliar documentação com código existente, registrar estado real, limites conhecidos e os achados de concorrência/autenticação.
- **Arquivos/módulos:** `docs/architecture.md`, possivelmente `docs/api.md`, `docs/frontend.md` e README.
- **Riscos:** documentação pode ficar obsoleta novamente; baixo risco operacional.
- **Validação:** conferir cada afirmação contra rotas, dependências e testes atuais; revisão de links e consistência cruzada.

## Revisão por área

- **Arquitetura/código:** organização por domínio e camadas claras; dependências entre módulos são geridas por serviços/evento. Acoplamento e duplicação graves não foram evidenciados na amostra revisada. A documentação contraditória é dívida concreta.
- **Frontend/UI/UX:** há rotas para descoberta, login, detalhes, ingressos, organizador e check-in. CSS tem foco visível, alvos de toque e redução de movimento. Não fiz auditoria visual em navegador nem avaliação completa de leitores de tela; não declarar conformidade de acessibilidade. Não há script/testes de frontend além de typecheck/build.
- **Backend/API:** DTOs usam validação, autorização ocorre no backend e erros usam envelope/request ID. O dashboard usa agregações SQL/JPA. O gateway é explicitamente simulado. Verificar limites de paginação e consistência de contratos em revisão específica.
- **Banco:** Flyway é fonte de schema, Hibernate valida, UUID, FKs, checks e índices explícitos são usados; migrações existentes devem permanecer imutáveis. A FK de usuários/emails e os caminhos de atualização merecem a verificação indicada acima. Não foram executadas queries de produção nem analisados planos de execução.
- **Concorrência:** estoque de compra usa update condicional; check-in usa update condicional e unicidade; pagamento usa lock. A proteção não foi estendida uniformemente a cancelamento/expiração (achado alto).
- **Segurança:** JWT curto, BCrypt, refresh token opaco/hash, autorização por papel/propriedade e CORS explícito são positivos. Pagamento aprovado pelo cliente é risco alto para qualquer uso transacional real; localStorage e ausência aparente de rate limiting são riscos médios.
- **Infra/observabilidade:** Docker Compose, health endpoint, shutdown gracioso, logs e CI com PostgreSQL existem. Não há evidência nesta inspeção de métricas/tracing/alertas. `render.yaml` tem `autoDeploy: true` e o health check depende do serviço; nenhuma configuração foi alterada. Deploy/CI e comportamento de migrations precisam de aprovação/ambiente de staging antes de mudanças operacionais.
- **Testes:** existem testes unitários de vários serviços/controladores e validação de schema com PostgreSQL no CI. Não encontrei testes de integração concorrente de corrida nem testes automatizados frontend/E2E. Nenhum teste foi executado nesta etapa de auditoria.

## Plano incremental proposto

1. **Concorrência de pedido** (branch atual `improvement/concurrency`): após aprovação, serializar cancelamento e expiração com pagamento; adicionar testes de regressão de concorrência e revisar cancelamento de evento.
2. **Rotação de refresh token** (`improvement/security`): rotação atômica e teste de reuso concorrente.
3. **Contrato de pagamento** (`improvement/security`): definir se a aplicação seguirá como demo ou aceitará gateway real antes de mudar o contrato público.
4. **Frontend** (`improvement/test-coverage` e `improvement/ui-ux`): primeiro adicionar cobertura de fluxos críticos; depois corrigir achados observáveis de responsividade/acessibilidade.
5. **Banco/performance** (`improvement/database-optimization`): medir consultas e dados antes de qualquer índice/migration; aplicar apenas migration compatível e validada em staging.
6. **Documentação/observabilidade** (`improvement/architecture`): corrigir docs obsoletas e definir monitoramento/alertas com requisitos de deploy conhecidos.

Branches de melhorias comportamentais não devem ser mescladas/deployadas sem CI, revisão, aprovação dos riscos de produção e validação funcional em ambiente isolado. Não foi criado PR: a primeira correção ainda aguarda aprovação e validação.

## Acompanhamento

- Auditoria registrada: concluída nesta branch.
- Melhoria implementada nesta branch: transições de pedido agora compartilham lock pessimista; expiração seleciona IDs e recarrega/revalida sob lock; cancelamento do evento bloqueia pedidos antes da transição.
- Branches: `improvement/concurrency` (a partir de `origin/main`).
- PRs: nenhum ainda; o teste PostgreSQL concorrente precisa rodar no CI/ambiente PostgreSQL isolado antes de considerar o PR validado.
- Testes executados: backend `mvn test` e `mvn verify` passaram com 111 testes (0 falhas); 5 testes PostgreSQL foram pulados porque não havia banco disponível nem daemon Docker ativo. Frontend `npm run typecheck` e `npm run build` passaram.
- Pendências prioritárias: executar o teste de concorrência e validação de schema em PostgreSQL; decisão sobre confiança do gateway simulado; rotação atômica do refresh token.
- Riscos que requerem aprovação: qualquer gateway financeiro, migration, deploy ou configuração de produção. A correção de concorrência foi aprovada pelo usuário nesta conversa e está isolada nesta branch; não foi implantada.
