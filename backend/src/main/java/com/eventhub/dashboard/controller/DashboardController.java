package com.eventhub.dashboard.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhub.dashboard.dto.DashboardResponse;
import com.eventhub.dashboard.dto.EventDashboardResponse;
import com.eventhub.dashboard.service.DashboardService;
import com.eventhub.security.AuthPrincipal;

@RestController
@RequestMapping("/api/organizer/dashboard")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse organizer(@AuthenticationPrincipal AuthPrincipal principal) {
        return dashboardService.organizer(principal);
    }

    @GetMapping("/events/{eventId}")
    public EventDashboardResponse event(@PathVariable UUID eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return dashboardService.event(eventId, principal);
    }
}
