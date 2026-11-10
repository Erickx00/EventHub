package com.eventhub.ticket.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tipo de ingresso de um evento (Pista, VIP, Camarote...).
 *
 * O estoque e um contador: total_quantity e available_quantity. A baixa real
 * acontece por UPDATE condicional no repositorio; os metodos aqui validam
 * regras que nao dependem de concorrencia (janela de venda, limite por pedido).
 */
@Entity
@Table(name = "ticket_types")
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 400)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "max_per_order", nullable = false)
    private int maxPerOrder;

    @Column(name = "sales_start", nullable = false)
    private Instant salesStart;

    @Column(name = "sales_end", nullable = false)
    private Instant salesEnd;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TicketType() {
        // exigido pelo JPA
    }

    private TicketType(UUID eventId, String name, String description, BigDecimal price, int totalQuantity,
            int maxPerOrder, Instant salesStart, Instant salesEnd) {
        this.eventId = eventId;
        this.name = name.trim();
        this.description = description;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
        this.maxPerOrder = maxPerOrder;
        this.salesStart = salesStart;
        this.salesEnd = salesEnd;
    }

    public static TicketType create(UUID eventId, String name, String description, BigDecimal price, int totalQuantity,
            int maxPerOrder, Instant salesStart, Instant salesEnd) {
        validateName(name);
        validatePrice(price);
        validateWindow(salesStart, salesEnd);
        validateTotal(totalQuantity, maxPerOrder);

        return new TicketType(eventId, name, description, price, totalQuantity, maxPerOrder, salesStart, salesEnd);
    }

    /**
     * Aumentar a quantidade libera mais estoque; reduzir so e permitido ate o
     * numero ja vendido, para nao invalidar compras confirmadas.
     */
    public void updateDetails(String name, String description, BigDecimal price, int newTotalQuantity,
            int maxPerOrder, Instant salesStart, Instant salesEnd) {
        validateName(name);
        validatePrice(price);
        validateWindow(salesStart, salesEnd);
        validateTotal(newTotalQuantity, maxPerOrder);

        int sold = totalQuantity - availableQuantity;
        if (newTotalQuantity < sold) {
            throw BusinessException.conflict(ErrorCode.INVALID_TICKET_TYPE,
                    "Nao e possivel reduzir abaixo de " + sold + " ingressos ja vendidos.");
        }

        this.name = name.trim();
        this.description = description;
        this.price = price;
        this.totalQuantity = newTotalQuantity;
        this.availableQuantity = newTotalQuantity - sold;
        this.maxPerOrder = maxPerOrder;
        this.salesStart = salesStart;
        this.salesEnd = salesEnd;
    }

    public boolean hasSales() {
        return availableQuantity < totalQuantity;
    }

    public boolean isOnSaleAt(Instant now) {
        return !now.isBefore(salesStart) && now.isBefore(salesEnd);
    }

    /**
     * Validacoes de entrada de um pedido. A garantia contra oversell continua
     * sendo o UPDATE condicional: entre esta checagem e a baixa, outra transacao
     * pode consumir o estoque restante.
     */
    public void assertPurchasable(int quantity, Instant now) {
        if (quantity <= 0) {
            throw BusinessException.badRequest(ErrorCode.INVALID_TICKET_TYPE, "Quantidade deve ser positiva.");
        }
        if (!isOnSaleAt(now)) {
            throw BusinessException.conflict(ErrorCode.SALES_CLOSED, "Vendas encerradas para este ingresso.");
        }
        if (quantity > maxPerOrder) {
            throw BusinessException.conflict(ErrorCode.TICKET_LIMIT_EXCEEDED,
                    "Limite de " + maxPerOrder + " ingressos por pedido.");
        }
        if (quantity > availableQuantity) {
            throw BusinessException.conflict(ErrorCode.TICKET_SOLD_OUT, "Estoque insuficiente.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 80) {
            throw BusinessException.badRequest(ErrorCode.INVALID_TICKET_TYPE,
                    "Nome do ingresso e obrigatorio (maximo 80 caracteres).");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw BusinessException.badRequest(ErrorCode.INVALID_TICKET_TYPE, "Preco nao pode ser negativo.");
        }
    }

    private static void validateWindow(Instant salesStart, Instant salesEnd) {
        if (salesStart == null || salesEnd == null || !salesEnd.isAfter(salesStart)) {
            throw BusinessException.badRequest(ErrorCode.INVALID_TICKET_TYPE,
                    "O fim das vendas deve ser posterior ao inicio.");
        }
    }

    private static void validateTotal(int totalQuantity, int maxPerOrder) {
        if (totalQuantity <= 0) {
            throw BusinessException.badRequest(ErrorCode.INVALID_TICKET_TYPE, "Quantidade total deve ser positiva.");
        }
        if (maxPerOrder <= 0 || maxPerOrder > totalQuantity) {
            throw BusinessException.badRequest(ErrorCode.INVALID_TICKET_TYPE,
                    "Limite por pedido deve estar entre 1 e a quantidade total.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getMaxPerOrder() {
        return maxPerOrder;
    }

    public Instant getSalesStart() {
        return salesStart;
    }

    public Instant getSalesEnd() {
        return salesEnd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
