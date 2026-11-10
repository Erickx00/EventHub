package com.eventhub.order.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Pedido: agregado de itens com uma maquina de estados propria.
 *
 * PENDING  -> PAID      (pagamento aprovado)
 * PENDING  -> EXPIRED   (prazo vencido, job)
 * PENDING  -> CANCELLED (usuario desiste)
 * Estados finais nao voltam atras.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // exigido pelo JPA
    }

    private Order(UUID userId, UUID eventId, Instant expiresAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.status = OrderStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public static Order create(UUID userId, UUID eventId, Instant expiresAt) {
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw BusinessException.badRequest(ErrorCode.INVALID_PERIOD, "Prazo de pagamento invalido.");
        }
        return new Order(userId, eventId, expiresAt);
    }

    /**
     * O total e sempre calculado aqui, a partir do preco vindo do banco.
     * Jamais use um total enviado pelo cliente.
     */
    public void addItem(UUID ticketTypeId, int quantity, BigDecimal unitPrice) {
        requirePending();
        if (quantity <= 0) {
            throw BusinessException.badRequest(ErrorCode.EMPTY_ORDER, "Quantidade deve ser positiva.");
        }
        items.add(new OrderItem(this, ticketTypeId, quantity, unitPrice));
        totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public void markPaid() {
        requirePending();
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        requirePending();
        this.status = OrderStatus.CANCELLED;
    }

    public void expire() {
        requirePending();
        this.status = OrderStatus.EXPIRED;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isOverdueAt(Instant now) {
        return isPending() && !now.isBefore(expiresAt);
    }

    private void requirePending() {
        if (status != OrderStatus.PENDING) {
            throw BusinessException.conflict(ErrorCode.ORDER_NOT_PENDING,
                    "Pedido nao esta mais pendente (status atual: " + status + ").");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
