package com.eventhub.ticket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhub.ticket.entity.TicketType;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    List<TicketType> findAllByEventIdOrderByPriceAsc(UUID eventId);

    /**
     * Baixa de estoque atomica. O WHERE condicional e o que impede oversell: se
     * outra transacao consumiu o ultimo ingresso, nenhuma linha e atualizada e o
     * retorno e 0. Ler o saldo em Java e depois gravar seria uma corrida.
     *
     * @return linhas afetadas (0 = estoque insuficiente)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TicketType t
               set t.availableQuantity = t.availableQuantity - :quantity
             where t.id = :id
               and t.availableQuantity >= :quantity
            """)
    int reserveStock(@Param("id") UUID id, @Param("quantity") int quantity);

    /**
     * Devolve estoque (pedido expirado ou cancelado). O teto impede que uma
     * devolucao duplicada ultrapasse a quantidade total.
     *
     * @return linhas afetadas (0 = nada a devolver)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TicketType t
               set t.availableQuantity = t.availableQuantity + :quantity
             where t.id = :id
               and t.availableQuantity + :quantity <= t.totalQuantity
            """)
    int releaseStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
