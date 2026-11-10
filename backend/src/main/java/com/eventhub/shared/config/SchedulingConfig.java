package com.eventhub.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita @Scheduled. Hoje o unico consumidor e o job de expiracao de pedidos.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
