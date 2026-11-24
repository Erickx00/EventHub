package com.eventhub.order.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.event.domain.EventCancelled;
import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderItem;
import com.eventhub.order.entity.OrderStatus;
import com.eventhub.order.repository.OrderRepository;
import com.eventhub.payment.service.PaymentService;
import com.eventhub.ticket.service.TicketService;
import com.eventhub.ticket.service.TicketTypeService;

/**
 * Reage ao cancelamento do evento. Roda na mesma transacao do cancelamento:
 * ou o evento e os pedidos/ingressos mudam juntos, ou nada muda.
 *
 * O listener existe para o modulo de eventos nao precisar conhecer pedidos,
 * ingressos e pagamentos — sem isso haveria ciclo de beans.
 */
@Component
public class EventCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(EventCancelledListener.class);

    private final OrderRepository orders;
    private final TicketTypeService ticketTypeService;
    private final TicketService ticketService;
    private final PaymentService paymentService;

    public EventCancelledListener(OrderRepository orders,
            TicketTypeService ticketTypeService,
            TicketService ticketService,
            PaymentService paymentService) {
        this.orders = orders;
        this.ticketTypeService = ticketTypeService;
        this.ticketService = ticketService;
        this.paymentService = paymentService;
    }

    @EventListener
    @Transactional
    public void handle(EventCancelled event) {
        int cancelledOrders = 0;
        int refundedOrders = 0;

        // Pending: devolve o estoque reservado.
        for (Order order : orders.findAllByEventIdAndStatusForUpdate(event.eventId(), OrderStatus.PENDING)) {
            order.cancel();
            for (OrderItem item : order.getItems()) {
                ticketTypeService.release(item.getTicketTypeId(), item.getQuantity());
            }
            cancelledOrders++;
        }

        // Paid: cancela e estorna. O ingresso e invalidado no lote abaixo.
        for (Order order : orders.findAllByEventIdAndStatusForUpdate(event.eventId(), OrderStatus.PAID)) {
            order.cancelAfterPayment();
            paymentService.refund(order);
            refundedOrders++;
        }

        int cancelledTickets = ticketService.cancelForEvent(event.eventId());

        log.info("Evento {} cancelado: {} pedido(s) pendente(s), {} pedido(s) estornado(s), {} ingresso(s) invalidado(s).",
                event.eventId(), cancelledOrders, refundedOrders, cancelledTickets);
    }
}
