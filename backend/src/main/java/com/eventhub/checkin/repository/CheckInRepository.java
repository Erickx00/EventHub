package com.eventhub.checkin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhub.checkin.entity.CheckIn;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    boolean existsByTicketId(UUID ticketId);
}
