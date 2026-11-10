package com.eventhub.order.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Linha do pedido. Guarda o preco unitario do momento da compra: se o
 * organizador mudar o preco do ingresso depois, o historico nao muda.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItem() {
        // exigido pelo JPA
    }

    OrderItem(Order order, UUID ticketTypeId, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.ticketTypeId = ticketTypeId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketTypeId() {
        return ticketTypeId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
