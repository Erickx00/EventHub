package com.eventhub.checkin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhub.checkin.entity.CheckIn;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    boolean existsByTicketId(UUID ticketId);

    @Query("""
            select count(c)
              from CheckIn c, Ticket t, Event e
             where c.ticketId = t.id
               and t.eventId = e.id
               and e.organizerId = :organizerId
            """)
    long countByOrganizer(@Param("organizerId") UUID organizerId);

    @Query("""
            select count(c)
              from CheckIn c, Ticket t
             where c.ticketId = t.id
               and t.eventId = :eventId
            """)
    long countByEvent(@Param("eventId") UUID eventId);
}
