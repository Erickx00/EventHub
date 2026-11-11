# Ingresso digital e check-in

## Emissão

Quando o pagamento é aprovado, o pedido vira `PAID` e os ingressos são emitidos **na mesma
transação**: se a emissão falhar, o pedido não fica pago sem ingresso.

Um ingresso por unidade comprada. Cada um tem um `qr_token` aleatório de 32 bytes
(Base64URL), único no banco.

O QR Code codifica **apenas esse token**. Nenhum nome, e-mail ou documento — quem fotografar
o código obtém um identificador opaco que só faz sentido contra o backend.

## Validação

```
POST /api/checkins/validate  { "qrToken": "..." }
```

Sequência:

1. token existe? → senão `404 INVALID_QR_TOKEN`
2. quem valida gerencia o evento? → senão `403 FORBIDDEN`
3. `UPDATE tickets SET status='USED' WHERE id=? AND status='VALID'`
4. 0 linhas afetadas → `409 TICKET_ALREADY_USED`
5. grava `check_ins`

O passo 3 é o veredito. Ler o status em Java e depois gravar deixaria dois leitores simultâneos
do mesmo QR Code passarem os dois — o clássico "entrou duas vezes com o mesmo ingresso".

O banco reforça: `check_ins.ticket_id` é `UNIQUE`. Mesmo que a validação atômica fosse
removida por engano, o segundo check-in viola a constraint.

## Contrato

| Método | Endpoint | Acesso |
| --- | --- | --- |
| GET | `/api/tickets/me` | autenticado |
| GET | `/api/tickets/{id}` | dono (404 para terceiros) |
| GET | `/api/tickets/{id}/qr` | dono — PNG |
| POST | `/api/checkins/validate` | `ORGANIZER`/`ADMIN` do evento |

## Limites conhecidos

- ingressos de pedido pago não são cancelados automaticamente quando o evento é cancelado
  (falta o fluxo de reembolso/estorno);
- não há `check_ins` offline nem sincronização: validação exige rede;
- o QR é gerado sob demanda a cada `GET /qr` — não há cache nem arquivo em disco.
