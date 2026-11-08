package com.eventhub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eventhub.user.entity.Role;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-long!!";

    @Test
    void issuesTokenCarryingIdentity() {
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(15));
        UUID userId = UUID.randomUUID();

        String token = service.issue(userId, "ana@example.com", Role.ORGANIZER);

        AuthPrincipal principal = service.parse(token);
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("ana@example.com");
        assertThat(principal.role()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = new JwtService(SECRET, Duration.ofSeconds(-60));

        String token = service.issue(UUID.randomUUID(), "ana@example.com", Role.ATTENDEE);

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService issuer = new JwtService(SECRET, Duration.ofMinutes(15));
        JwtService other = new JwtService("another-secret-with-at-least-32-bytes-ok", Duration.ofMinutes(15));

        String token = issuer.issue(UUID.randomUUID(), "ana@example.com", Role.ATTENDEE);

        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesWeakSecret() {
        assertThatThrownBy(() -> new JwtService("curto", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
