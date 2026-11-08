package com.eventhub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eventhub.auth.dto.AuthResponse;
import com.eventhub.auth.entity.RefreshToken;
import com.eventhub.auth.repository.RefreshTokenRepository;
import com.eventhub.security.JwtService;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.user.entity.Role;
import com.eventhub.user.entity.User;
import com.eventhub.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private RefreshTokenRepository refreshTokens;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, refreshTokens, passwordEncoder, jwtService, Duration.ofDays(7));
    }

    private void stubTokens() {
        when(jwtService.issue(any(), anyString(), any())).thenReturn("access-token");
        when(jwtService.accessTtl()).thenReturn(Duration.ofMinutes(15));
    }

    @Test
    void registersAttendeeByDefault() {
        when(users.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTokens();

        AuthResponse response = authService.register("Ana", "  Ana@Example.com ", "senha1234", null);

        assertThat(response.user().email()).isEqualTo("ana@example.com");
        assertThat(response.user().role()).isEqualTo(Role.ATTENDEE);
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("Ana", "ana@example.com", "senha1234", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EMAIL_ALREADY_USED);
    }

    @Test
    void refusesSelfRegistrationAsAdmin() {
        assertThatThrownBy(() -> authService.register("Ana", "ana@example.com", "senha1234", Role.ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN);
    }

    @Test
    void logsInWithValidCredentials() {
        User user = User.create("Ana", "ana@example.com", "bcrypt-hash", Role.ORGANIZER);
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha1234", "bcrypt-hash")).thenReturn(true);
        stubTokens();

        AuthResponse response = authService.login("ANA@example.com", "senha1234");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().role()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void reportsSameErrorForUnknownEmailAndWrongPassword() {
        User user = User.create("Ana", "ana@example.com", "bcrypt-hash", Role.ATTENDEE);
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ana@example.com", "errada"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_CREDENTIALS);

        when(users.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ninguem@example.com", "senha1234"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void refreshRotatesTheToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored = RefreshToken.issue(userId, AuthService.hash("token-antigo"), Instant.now().plusSeconds(600));
        when(refreshTokens.findByTokenHash(AuthService.hash("token-antigo"))).thenReturn(Optional.of(stored));
        when(users.findById(userId)).thenReturn(Optional.of(User.create("Ana", "ana@example.com", "hash", Role.ATTENDEE)));
        stubTokens();

        AuthResponse response = authService.refresh("token-antigo");

        assertThat(stored.getRevokedAt()).isNotNull();
        assertThat(response.refreshToken()).isNotEqualTo("token-antigo");
    }

    @Test
    void rejectsExpiredRefreshToken() {
        RefreshToken expired = RefreshToken.issue(UUID.randomUUID(), AuthService.hash("velho"), Instant.now().minusSeconds(1));
        when(refreshTokens.findByTokenHash(AuthService.hash("velho"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("velho"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    void rejectsUnknownRefreshToken() {
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("inexistente"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN);
    }

    @Test
    void logoutIsIdempotent() {
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout("ja-revogado");

        verify(refreshTokens, never()).save(any());
    }

    @Test
    void changingPasswordRevokesEverySession() {
        UUID userId = UUID.randomUUID();
        User user = User.create("Ana", "ana@example.com", "hash-antigo", Role.ATTENDEE);
        RefreshToken sessionA = RefreshToken.issue(userId, "hash-a", Instant.now().plusSeconds(600));
        RefreshToken sessionB = RefreshToken.issue(userId, "hash-b", Instant.now().plusSeconds(600));

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha1234", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.encode("senhaNova123")).thenReturn("hash-novo");
        when(refreshTokens.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(sessionA, sessionB));

        authService.changePassword(userId, "senha1234", "senhaNova123");

        assertThat(user.getPasswordHash()).isEqualTo("hash-novo");
        assertThat(sessionA.getRevokedAt()).isNotNull();
        assertThat(sessionB.getRevokedAt()).isNotNull();
    }

    @Test
    void refusesPasswordChangeWithWrongCurrentPassword() {
        UUID userId = UUID.randomUUID();
        User user = User.create("Ana", "ana@example.com", "hash-antigo", Role.ATTENDEE);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(userId, "errada", "senhaNova123"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PASSWORD_MISMATCH);
    }
}
