package com.eventhub.ticket.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhub.ticket.entity.Ticket;
import com.eventhub.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findAllByUserIdOrderByIssuedAtDesc(UUID userId);

    Optional<Ticket> findByQrToken(String qrToken);

    /**
     * Validacao atomica: so sai de VALID uma vez. Dois leitores simultaneos do
     * mesmo QR Code fazem o update, mas apenas um obtem 1 linha afetada.
     *
     * @return linhas afetadas (0 = ja usado ou cancelado)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Ticket t
               set t.status = :used
             where t.id = :id
               and t.status = :valid
            """)
    int markUsed(@Param("id") UUID id, @Param("used") TicketStatus used, @Param("valid") TicketStatus valid);

    @Query("""
            select count(t)
              from Ticket t, Event e
             where t.eventId = e.id
               and e.organizerId = :organizerId
            """)
    long countByOrganizer(@Param("organizerId") UUID organizerId);

    @Query("""
            select count(distinct t.userId)
              from Ticket t, Event e
             where t.eventId = e.id
               and e.organizerId = :organizerId
            """)
    long countDistinctAttendeesByOrganizer(@Param("organizerId") UUID organizerId);

    long countByEventId(UUID eventId);

    @Query("select count(distinct t.userId) from Ticket t where t.eventId = :eventId")
    long countDistinctAttendeesByEvent(@Param("eventId") UUID eventId);
}
