package com.eventhub.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.user.entity.Role;
import com.eventhub.user.entity.User;
import com.eventhub.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private UserService userService;

    @Test
    void renamesUser() {
        UUID userId = UUID.randomUUID();
        User user = User.create("Ana", "ana@example.com", "hash", Role.ATTENDEE);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        var response = userService.updateProfile(userId, "  Ana Maria  ");

        assertThat(response.name()).isEqualTo("Ana Maria");
    }

    @Test
    void failsForUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.USER_NOT_FOUND);
    }
}
