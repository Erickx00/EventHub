package com.eventhub.payment.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tentativa de pagamento. Um pedido pode acumular uma recusada e depois uma
 * aprovada: por isso o historico fica em linhas separadas.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
        // exigido pelo JPA
    }

    private Payment(UUID orderId, BigDecimal amount, PaymentStatus status, PaymentMethod method, Instant paidAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.paidAt = paidAt;
    }

    public static Payment approved(UUID orderId, BigDecimal amount, PaymentMethod method, Instant paidAt) {
        return new Payment(orderId, amount, PaymentStatus.APPROVED, method, paidAt);
    }

    public static Payment refused(UUID orderId, BigDecimal amount, PaymentMethod method) {
        return new Payment(orderId, amount, PaymentStatus.REFUSED, method, null);
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
