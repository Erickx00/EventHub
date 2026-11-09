package com.eventhub.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.eventhub.event.dto.CreateEventRequest;
import com.eventhub.event.dto.EventResponse;
import com.eventhub.event.dto.UpdateEventRequest;
import com.eventhub.event.entity.Category;
import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventStatus;
import com.eventhub.event.repository.EventRepository;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.user.entity.Role;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Instant START = Instant.now().plus(30, ChronoUnit.DAYS);
    private static final Instant END = START.plus(8, ChronoUnit.HOURS);

    @Mock
    private EventRepository events;

    private EventService eventService;

    private final UUID organizerId = UUID.randomUUID();
    private final AuthPrincipal organizer = new AuthPrincipal(organizerId, "org@example.com", Role.ORGANIZER);

    @BeforeEach
    void setUp() {
        eventService = new EventService(events);
    }

    private CreateEventRequest createRequest() {
        return new CreateEventRequest("Java Conference", "Evento sobre Java", Category.TECHNOLOGY,
                "Campina Grande", "Teatro Municipal", START, END);
    }

    private Event draftOwnedByOrganizer() {
        return Event.draft(organizerId, "Java Conference", "Evento sobre Java", Category.TECHNOLOGY,
                "Campina Grande", "Teatro Municipal", START, END);
    }

    private UpdateEventRequest updateRequest() {
        return new UpdateEventRequest("Evento Atualizado", "Nova descricao", Category.MUSIC,
                "Recife", null, START, END);
    }

    @Test
    void createsDraftOwnedByAuthenticatedOrganizer() {
        when(events.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.create(createRequest(), organizer);

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(events).save(saved.capture());
        assertThat(saved.getValue().getOrganizerId()).isEqualTo(organizerId);
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(response.city()).isEqualTo("Campina Grande");
    }

    @Test
    void refusesCreationByAttendee() {
        AuthPrincipal attendee = new AuthPrincipal(UUID.randomUUID(), "ana@example.com", Role.ATTENDEE);

        assertThatThrownBy(() -> eventService.create(createRequest(), attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN);

        verify(events, never()).save(any());
    }

    @Test
    void refusesEventStartingInThePast() {
        CreateEventRequest past = new CreateEventRequest("Retrospective", null, Category.TECHNOLOGY,
                "Recife", null, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> eventService.create(past, organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_PERIOD);
    }

    @Test
    void refusesPeriodThatEndsBeforeItStarts() {
        CreateEventRequest inverted = new CreateEventRequest("Invertido", null, Category.TECHNOLOGY,
                "Recife", null, START, START.minus(1, ChronoUnit.HOURS));

        assertThatThrownBy(() -> eventService.create(inverted, organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_PERIOD);
    }

    @Test
    void refusesUpdateByAnotherOrganizer() {
        UUID id = UUID.randomUUID();
        Event event = draftOwnedByOrganizer();
        when(events.findById(id)).thenReturn(Optional.of(event));

        AuthPrincipal other = new AuthPrincipal(UUID.randomUUID(), "outro@example.com", Role.ORGANIZER);

        assertThatThrownBy(() -> eventService.update(id, updateRequest(), other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN);
    }

    @Test
    void allowsAdminToManageSomeoneElsesEvent() {
        UUID id = UUID.randomUUID();
        Event event = draftOwnedByOrganizer();
        when(events.findById(id)).thenReturn(Optional.of(event));

        AuthPrincipal admin = new AuthPrincipal(UUID.randomUUID(), "admin@example.com", Role.ADMIN);

        EventResponse response = eventService.update(id, updateRequest(), admin);

        assertThat(response.name()).isEqualTo("Evento Atualizado");
    }

    @Test
    void publishesDraftOnlyOnce() {
        UUID id = UUID.randomUUID();
        Event event = draftOwnedByOrganizer();
        when(events.findById(id)).thenReturn(Optional.of(event));

        EventResponse published = eventService.publish(id, organizer);
        assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);

        assertThatThrownBy(() -> eventService.publish(id, organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EVENT_NOT_PUBLISHABLE);
    }

    @Test
    void cancellingTwiceIsIdempotent() {
        UUID id = UUID.randomUUID();
        Event event = draftOwnedByOrganizer();
        when(events.findById(id)).thenReturn(Optional.of(event));

        eventService.cancel(id, organizer);
        eventService.cancel(id, organizer);

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    void hidesDraftFromAnonymousVisitors() {
        UUID id = UUID.randomUUID();
        when(events.findById(id)).thenReturn(Optional.of(draftOwnedByOrganizer()));

        assertThatThrownBy(() -> eventService.getById(id, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EVENT_NOT_FOUND);
    }

    @Test
    void showsDraftToItsOwner() {
        UUID id = UUID.randomUUID();
        when(events.findById(id)).thenReturn(Optional.of(draftOwnedByOrganizer()));

        EventResponse response = eventService.getById(id, organizer);

        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void searchMapsPageAndKeepsPaginationMetadata() {
        Event event = draftOwnedByOrganizer();
        event.publish();
        Pageable pageable = PageRequest.of(1, 5);
        Page<Event> page = new PageImpl<>(List.of(event), pageable, 12);
        when(events.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var response = eventService.search(Category.TECHNOLOGY, "campina grande", START, END, "java", pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(12);
        assertThat(response.totalPages()).isEqualTo(3);
    }
}
