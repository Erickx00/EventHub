# Pedidos, pagamentos e expiração

## Ciclo de vida

```
                 pagamento aprovado
   PENDING ───────────────────────────► PAID        (final)
      │
      │ 15 min sem pagar (job)
      ├────────────────────────────────► EXPIRED     (final, estoque devolvido)
      │
      │ usuário cancela
      └────────────────────────────────► CANCELLED   (final, estoque devolvido)
```

Estados finais não voltam atrás: `Order.markPaid()`, `cancel()` e `expire()` chamam
`requirePending()` e respondem `409 ORDER_NOT_PENDING` se o pedido já saiu de `PENDING`.

## O que o cliente manda — e o que ele não manda

O corpo traz apenas **evento, tipo de ingresso e quantidade**:

```json
{ "eventId": "9f2c...", "items": [{ "ticketTypeId": "6b1d...", "quantity": 2 }] }
```

Preço unitário e total vêm do banco. Aceitar `total` do cliente é a falha clássica que
permite comprar VIP por R$ 1,00 — e o valor congelado em `order_items.unit_price` garante que
mudar o preço depois não reescreve o histórico.

## Reserva e rollback

Criar pedido faz, na mesma transação: validar evento publicado → validar tipo pertence ao
evento → `assertPurchasable` → baixa atômica de estoque → montar o agregado. Se qualquer linha
falhar, o `@Transactional` desfaz as baixas anteriores. Não existe estado intermediário com
estoque consumido e pedido inexistente.

Linhas duplicadas do mesmo tipo são somadas antes da reserva, senão o limite por pedido seria
furado enviando o mesmo item duas vezes.

## Pagamento simulado

`PaymentOutcome` (`APPROVED`/`REFUSED`) é o controle do simulador e **só existe porque não há
gateway real**. O contrato já está atrás de `PaymentGateway`; ao integrar um provedor, o campo sai
do payload e o status passa a vir da resposta dele.

Duas decisões que evitam bug:

**Lock pessimista na leitura do pedido.** Dois cliques simultâneos no "pagar" serializam em
`findByIdForUpdate`; o segundo encontra `PAID` e recebe `409`.

**Pedido vencido não é mutado dentro do pagamento.** Marcar `EXPIRED` e lançar exceção na mesma
transação faria o rollback desfazer a própria expiração. O pagamento apenas responde
`409 ORDER_EXPIRED`; quem faz a transição e devolve o estoque é o job.

Pagamento recusado **mantém o pedido em `PENDING`**, para nova tentativa dentro do prazo.

**Evento cancelado ou despublicado depois do pedido aberto recusa o pagamento** com
`409 EVENT_NOT_PUBLISHED`. Sem essa checagem, um organizador que cancela o evento ainda receberia
dinheiro de pedidos abertos minutos antes — o pior tipo de inconsistência: pago, sem evento.

## Expiração

`OrderExpirationJob` roda a cada `eventhub.orders.expiry-scan-ms` (60s por padrão), busca
`PENDING` com `expires_at` no passado, marca `EXPIRED` e devolve o estoque de cada item.

Limites conhecidos:

- o lote roda em uma transação só — se um item falhar, a passagem inteira reverte e o próximo
  ciclo tenta de novo;
- o estoque de um pedido vencido fica preso até a próxima passagem (no máximo 60s);
- não há estorno: `REFUNDED` existe no enum mas o fluxo de reembolso é depois da etapa 6.
