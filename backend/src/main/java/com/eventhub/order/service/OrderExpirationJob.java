package com.eventhub.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Devolve estoque de pedidos nao pagos. Roda a cada minuto por padrao; em caso
 * de restart, a proxima passagem resolve o que ficou pendente.
 */
@Component
public class OrderExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationJob.class);

    private final OrderService orderService;

    public OrderExpirationJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${eventhub.orders.expiry-scan-ms:60000}")
    public void expireOverdueOrders() {
        int expired = orderService.expireOverdue();
        if (expired > 0) {
            log.info("{} pedido(s) expirado(s); estoque devolvido.", expired);
        }
    }
}
