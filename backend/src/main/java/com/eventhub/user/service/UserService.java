package com.eventhub.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.user.dto.UserResponse;
import com.eventhub.user.entity.User;
import com.eventhub.user.repository.UserRepository;

/**
 * Perfil do usuario. Credenciais (senha, tokens) ficam no AuthService.
 */
@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, String name) {
        User user = requireUser(id);
        user.rename(name);
        return UserResponse.from(user);
    }

    private User requireUser(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.USER_NOT_FOUND, "Usuario nao encontrado."));
    }
}
