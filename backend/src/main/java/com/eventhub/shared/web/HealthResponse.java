package com.eventhub.shared.web;

import java.time.Instant;

/**
 * Resposta do health check. Nao expoe detalhes de infraestrutura: apenas o
 * suficiente para o frontend e o load balancer saberem que a API esta de pe.
 */
public record HealthResponse(String status, String service, Instant timestamp) {
}
