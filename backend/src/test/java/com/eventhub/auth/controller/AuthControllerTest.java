package com.eventhub.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventhub.auth.dto.AuthResponse;
import com.eventhub.auth.service.AuthService;
import com.eventhub.user.dto.UserResponse;
import com.eventhub.user.entity.Role;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registersAndReturnsTokens() throws Exception {
        when(authService.register(any(), any(), any(), any())).thenReturn(new AuthResponse(
                "Bearer", "access-token", "refresh-token", 900,
                new UserResponse(UUID.randomUUID(), "Ana", "ana@example.com", Role.ATTENDEE, Instant.now())));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ana","email":"ana@example.com","password":"senha1234"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.role").value("ATTENDEE"));
    }

    @Test
    void rejectsMalformedEmailWithErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ana","email":"nao-e-email","password":"senha1234"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ana","email":"ana@example.com","password":"123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
