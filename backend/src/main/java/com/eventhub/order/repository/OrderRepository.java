package com.eventhub.order.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderStatus;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Lock pessimista: duas tentativas de pagamento do mesmo pedido sao
     * serializadas, e a segunda encontra o status ja alterado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    Page<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Order> findAllByStatusAndExpiresAtBefore(OrderStatus status, Instant now);

    List<Order> findAllByEventIdAndStatus(UUID eventId, OrderStatus status);

    /**
     * Receita = soma do total de pedidos PAID dos eventos do organizador. O
     * total ja e a soma dos itens com preco congelado, entao nao ha dupla
     * contagem.
     */
    @Query("""
            select coalesce(sum(o.totalAmount), 0)
              from Order o, Event e
             where o.eventId = e.id
               and e.organizerId = :organizerId
               and o.status = :status
            """)
    BigDecimal sumRevenueByOrganizer(@Param("organizerId") UUID organizerId, @Param("status") OrderStatus status);

    @Query("""
            select coalesce(sum(o.totalAmount), 0)
              from Order o
             where o.eventId = :eventId
               and o.status = :status
            """)
    BigDecimal sumRevenueByEvent(@Param("eventId") UUID eventId, @Param("status") OrderStatus status);
}
