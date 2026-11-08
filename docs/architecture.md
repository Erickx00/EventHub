# Arquitetura

## Estilo: monólito modular

Um único processo Spring Boot, organizado por **módulos de negócio**. Cada módulo tem suas
camadas internas e conversa com os outros apenas por serviços públicos, nunca por
repositórios ou entidades alheias.

Por que não microsserviços agora: um projeto solo com 8 semanas ganha muito pouco com
comunicação remota, service discovery e consistência eventual, e perde velocidade e
capacidade de explicar o próprio sistema. O desenho modular permite extrair um serviço
depois, se fizer sentido, sem reescrever as regras de negócio.

## Camadas

| Camada | Responsabilidade | Não pode |
| --- | --- | --- |
| `controller` | Receber HTTP, validar entrada via Bean Validation, devolver DTO | conter regra de negócio |
| `dto` | Transportar dados de entrada e saída | expor entidades JPA |
| `service` | Casos de uso, transações e regras de negócio | conhecer HTTP |
| `domain`/`entity` | Entidades, enums e regras centrais | depender de Spring MVC |
| `repository` | Acesso a dados | conter regra de negócio |
| `security` | Autenticação e autorização | misturar-se com regra de módulo |
| `infrastructure` | E-mail, QR Code, gateways externos | ser chamado direto do controller |
| `exception` | Tratamento padronizado de erros | vazar stack trace para o cliente |

## Pacotes

```
com.eventhub
├── EventHubApplication.java
├── shared/{config,exception,response,util,web}
├── security/{config,jwt,filter,service}
├── user/{controller,dto,entity,repository,service}
├── event/{...}
├── ticket/{...}
├── order/{...}
├── payment/{controller,entity,service,gateway}
├── checkin/{...}
├── notification/{service,provider}
└── dashboard/{controller,service}
```

Estado atual (Sprint 1): apenas `EventHubApplication`, `shared/web` e a migração `V1`.
Os demais pacotes nascem nas etapas 2 a 7 — **sem classes vazias**.

## Fluxo de uma requisição

```
HTTP -> Controller -> Service -> Repository -> PostgreSQL
                \-> DTO (resposta)  \-> ExceptionHandler (erro)
```

Regra de ouro: o `Service` recebe a identidade do usuário autenticado (via
`SecurityContext`), nunca um `userId`/`organizerId` vindo do corpo da requisição. Isso
elimina uma classe inteira de falhas de autorização.

## Decisões

**UUID como chave primária.** IDs não são enumeráveis, o que evita varredura trivial de
`/api/orders/{id}` e facilita gerar registros antes de gravar. O custo é um índice maior
que um `bigserial`; aceitável nesta escala.

**O schema pertence ao Flyway.** `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate
apenas confere se as entidades batem com as migrações. Deploy não altera estrutura.

**Preço congelado no pedido.** `order_items.unit_price` guarda o preço do momento da
compra. Alterar o preço do ingresso depois não reescreve o histórico.

**Dinheiro em `BigDecimal`/`numeric`.** Nunca `double`. `numeric(12,2)` no banco.

**Tempo em UTC.** Colunas `timestamptz`; o frontend converte para o fuso local.

**Erros padronizados.** Um `@RestControllerAdvice` traduz exceções de domínio em um
envelope único (`docs/api.md`), com `traceId` para correlacionar log e resposta.

## Autenticação e autorização

| Peça | Decisão |
| --- | --- |
| Access token | JWT HS256, 15 min, claims `sub` (UUID), `email`, `role` |
| Refresh token | opaco (32 bytes), hash SHA-256 no banco, 7 dias, uso único |
| Senha | BCrypt custo 12 |
| Sessão | stateless; sem cookie, sem CSRF (apenas `Authorization: Bearer`) |
| Autorização | `@EnableMethodSecurity` + regra de propriedade no service |

Três armadilhas que custaram tempo e ficam registradas:

1. **Filtro como `@Bean` roda duas vezes.** Um `Filter` exposto como bean é registrado
   também como filtro de servlet pelo Boot. Ele acaba executando *antes* do
   `SecurityContextHolderFilter`, que recria o contexto vazio — a autenticação é
   descartada e tudo vira 401. Por isso `JwtAuthenticationFilter` é instanciado dentro
   do `SecurityConfig`, não é bean.
2. **`@WebMvcTest` escaneia `Filter` beans.** Pelo mesmo motivo acima, qualquer slice MVC
   tentava criar o filtro sem `JwtService`. Com o filtro fora do container, o slice
   voltou a subir.
3. **Mockito no JDK 24+.** O auto-attach do ByteBuddy usa um processo externo. Em
   ambiente restrito isso falha; `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
   com o `SubclassByteBuddyMockMaker` resolve sem instrumentação.

Autorização por papel é declarativa (`hasRole`), mas a checagem de **propriedade**
("este organizador é dono deste evento?") fica no `Service` e devolve `403`/`404` —
nunca no frontend e nunca no controller.

## O que ainda não existe (e por quê)

- Autenticação: entra na etapa 2, junto com a entidade `User`.
- Tratamento global de erros: só faz sentido quando houver o primeiro caso de uso com falha.
- Módulos de evento/ingresso/pedido: etapas 3 a 6.

Documentar o limite é parte do desenho: evita abstração especulativa.
