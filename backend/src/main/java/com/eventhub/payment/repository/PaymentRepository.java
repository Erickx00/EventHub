package com.eventhub.payment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhub.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
