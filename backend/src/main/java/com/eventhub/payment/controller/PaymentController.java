package com.eventhub.payment.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhub.payment.dto.PaymentRequest;
import com.eventhub.payment.dto.PaymentResponse;
import com.eventhub.payment.service.PaymentService;
import com.eventhub.security.AuthPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse pay(@PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return paymentService.pay(orderId, request, principal);
    }
}
