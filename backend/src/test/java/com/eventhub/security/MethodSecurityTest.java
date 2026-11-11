package com.eventhub.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventhub.user.entity.Role;

/**
 * Protege contra a regressao mais provavel de autorizacao: um 403 que vira 500
 * porque o @PreAuthorize lanca dentro do controller, depois da cadeia de filtros.
 */
@WebMvcTest(ProtectedTestController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class MethodSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void attendeeReceivesForbiddenInsteadOfServerError() throws Exception {
        when(jwtService.parse("token-attendee"))
                .thenReturn(new AuthPrincipal(UUID.randomUUID(), "ana@example.com", Role.ATTENDEE));

        mockMvc.perform(get("/api/organizer/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-attendee"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void anonymousReceivesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/organizer/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void organizerIsAllowed() throws Exception {
        when(jwtService.parse("token-organizer"))
                .thenReturn(new AuthPrincipal(UUID.randomUUID(), "org@example.com", Role.ORGANIZER));

        mockMvc.perform(get("/api/organizer/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-organizer"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownRouteReturnsNotFoundInsteadOfServerError() throws Exception {
        // Autenticado: rota inexistente nao pode vazar existencia, entao o 404 so
        // aparece depois de passar pela autenticacao.
        when(jwtService.parse("token-organizer"))
                .thenReturn(new AuthPrincipal(UUID.randomUUID(), "org@example.com", Role.ORGANIZER));

        mockMvc.perform(get("/api/rota-que-nao-existe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-organizer"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
