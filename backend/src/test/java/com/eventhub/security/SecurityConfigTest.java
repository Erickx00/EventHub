package com.eventhub.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventhub.shared.web.HealthController;
import com.eventhub.user.controller.UserController;
import com.eventhub.user.dto.UserResponse;
import com.eventhub.user.entity.Role;
import com.eventhub.user.service.UserService;

import io.jsonwebtoken.MalformedJwtException;

/**
 * Exercita a cadeia real de seguranca: sem token -> 401, token valido -> 200,
 * e a rota publica de health continua acessivel.
 */
@WebMvcTest({ UserController.class, HealthController.class })
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        when(jwtService.parse("token-quebrado")).thenThrow(new MalformedJwtException("invalido"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-quebrado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsRequestWithValidToken() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthPrincipal principal = new AuthPrincipal(userId, "ana@example.com", Role.ATTENDEE);
        when(jwtService.parse("token-bom")).thenReturn(principal);
        when(userService.getById(userId))
                .thenReturn(new UserResponse(userId, "Ana", "ana@example.com", Role.ATTENDEE, Instant.now()));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-bom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void healthEndpointStaysPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
