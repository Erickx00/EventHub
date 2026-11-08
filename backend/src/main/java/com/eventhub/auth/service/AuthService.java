package com.eventhub.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.auth.dto.AuthResponse;
import com.eventhub.auth.entity.RefreshToken;
import com.eventhub.auth.repository.RefreshTokenRepository;
import com.eventhub.security.JwtService;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.user.dto.UserResponse;
import com.eventhub.user.entity.Role;
import com.eventhub.user.entity.User;
import com.eventhub.user.repository.UserRepository;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTtl;

    /**
     * Hash descartavel usado quando o e-mail nao existe: o login gasta o mesmo
     * tempo de BCrypt nos dois casos, para nao revelar quem tem conta.
     */
    private final String dummyHash;

    public AuthService(UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${eventhub.security.refresh-ttl}") Duration refreshTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTtl = refreshTtl;
        this.dummyHash = passwordEncoder.encode("hash-descartavel-para-tempo-constante");
    }

    @Transactional
    public AuthResponse register(String name, String email, String rawPassword, Role requestedRole) {
        Role role = resolveRole(requestedRole);

        String normalizedEmail = User.normalizeEmail(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw BusinessException.conflict(ErrorCode.EMAIL_ALREADY_USED, "E-mail ja cadastrado.");
        }

        User user = users.save(User.create(name, normalizedEmail, passwordEncoder.encode(rawPassword), role));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(String email, String rawPassword) {
        User user = users.findByEmail(User.normalizeEmail(email)).orElse(null);

        if (user == null) {
            // Mesmo custo de BCrypt do caminho valido, mesmo erro: nao revela quem tem conta.
            passwordEncoder.matches(rawPassword, dummyHash);
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return issueTokens(user);
    }

    /**
     * Rotacao: o refresh token usado e revogado e um novo par e emitido. Reuso de
     * um token antigo falha, o que detecta copia de credencial.
     */
    @Transactional
    public AuthResponse refresh(String rawToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> BusinessException.unauthorized(ErrorCode.INVALID_TOKEN, "Refresh token invalido."));

        Instant now = Instant.now();
        if (!stored.isActiveAt(now)) {
            throw BusinessException.unauthorized(ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Refresh token expirado ou revogado.");
        }

        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.USER_NOT_FOUND, "Usuario nao encontrado."));

        stored.revoke(now);
        return issueTokens(user);
    }

    /**
     * Idempotente: revogar um token ja revogado nao e erro, e logout nao pode
     * falhar por causa de retry do cliente.
     */
    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = users.findById(userId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.USER_NOT_FOUND, "Usuario nao encontrado."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw BusinessException.badRequest(ErrorCode.PASSWORD_MISMATCH, "Senha atual incorreta.");
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));

        // Trocar a senha encerra as outras sessoes.
        Instant now = Instant.now();
        refreshTokens.findAllByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(now));
    }

    private AuthResponse issueTokens(User user) {
        String rawRefreshToken = newOpaqueToken();
        refreshTokens.save(RefreshToken.issue(
                user.getId(),
                hash(rawRefreshToken),
                Instant.now().plus(refreshTtl)));

        return new AuthResponse(
                "Bearer",
                jwtService.issue(user.getId(), user.getEmail(), user.getRole()),
                rawRefreshToken,
                jwtService.accessTtl().toSeconds(),
                UserResponse.from(user));
    }

    private static Role resolveRole(Role requested) {
        if (requested == null) {
            return Role.ATTENDEE;
        }
        if (requested == Role.ADMIN) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "Nao e possivel se registrar como ADMIN.");
        }
        return requested;
    }

    private static BusinessException invalidCredentials() {
        return BusinessException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "E-mail ou senha invalidos.");
    }

    private static String newOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", ex);
        }
    }
}
