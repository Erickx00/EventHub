package com.eventhub.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fecha eventos que ja terminaram. Sem isso, um evento do mes passado continuaria
 * PUBLISHED e aparecendo em "descobrir eventos".
 */
@Component
public class EventClosingJob {

    private static final Logger log = LoggerFactory.getLogger(EventClosingJob.class);

    private final EventService eventService;

    public EventClosingJob(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(fixedDelayString = "${eventhub.events.finish-scan-ms:300000}")
    public void closeFinishedEvents() {
        int finished = eventService.finishOverdue();
        if (finished > 0) {
            log.info("{} evento(s) finalizado(s) por data de termino.", finished);
        }
    }
}
