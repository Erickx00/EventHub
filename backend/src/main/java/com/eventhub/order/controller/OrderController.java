package com.eventhub.order.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eventhub.order.dto.CreateOrderRequest;
import com.eventhub.order.dto.OrderResponse;
import com.eventhub.order.service.OrderService;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.response.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        OrderResponse created = orderService.create(request, principal);
        return ResponseEntity
                .created(URI.create("/api/orders/" + created.id()))
                .body(created);
    }

    @GetMapping("/me")
    public PageResponse<OrderResponse> mine(@AuthenticationPrincipal AuthPrincipal principal, Pageable pageable) {
        return orderService.findMine(principal, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return orderService.getById(id, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        orderService.cancel(id, principal);
    }
}
