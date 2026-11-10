# Ingressos e estoque

O estoque não é uma coluna decorativa: ele é um contador que só muda por `UPDATE` condicional.
Ler o saldo em Java e depois gravar é a corrida clássica que vende o mesmo assento duas vezes.

## O comando que impede o oversell

```sql
UPDATE ticket_types
   SET available_quantity = available_quantity - :quantity
 WHERE id = :id
   AND available_quantity >= :quantity;
```

Se nenhuma linha for atualizada, o estoque acabou entre a leitura e a escrita. Em JPQL isso vira
`TicketTypeRepository.reserveStock`, com retorno `int`:

```java
if (ticketTypes.reserveStock(id, quantity) == 0) {
    throw BusinessException.conflict(ErrorCode.TICKET_SOLD_OUT, "Estoque insuficiente.");
}
```

A devolução (pedido expirado ou cancelado) usa o mesmo padrão com teto no total, para que uma
devolução duplicada não crie estoque do nada.

## Regras que ficam na entidade

| Regra | Erro |
| --- | --- |
| Preço negativo | `400 INVALID_TICKET_TYPE` |
| Quantidade total ≤ 0 | `400 INVALID_TICKET_TYPE` |
| Limite por pedido fora de `1..total` | `400 INVALID_TICKET_TYPE` |
| Fim das vendas antes do início | `400 INVALID_TICKET_TYPE` |
| Compra fora da janela de venda | `409 SALES_CLOSED` |
| Quantidade acima do limite por pedido | `409 TICKET_LIMIT_EXCEEDED` |
| Quantidade acima do estoque | `409 TICKET_SOLD_OUT` |
| Reduzir total abaixo do já vendido | `409 INVALID_TICKET_TYPE` |
| Remover tipo que já tem venda | `409 TICKET_TYPE_HAS_SALES` |

Aumentar o total libera a diferença; reduzir recalcula `available = novoTotal - vendidos`, então
nunca invalida uma compra confirmada.

## Testes

`TicketTypeTest` cobre as regras da entidade e `TicketTypeServiceTest` cobre a orquestração —
incluindo o caso em que o `UPDATE` retorna `0` linhas (estoque levado por outra transação) e a
garantia de que a reserva **não** passa por `save()` da entidade.

O teste de concorrência real (duas threads, um ingresso) exige PostgreSQL e entra junto com os
testes de integração — hoje bloqueado porque o container não roda neste ambiente.
