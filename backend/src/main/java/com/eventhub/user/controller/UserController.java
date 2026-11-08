package com.eventhub.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhub.security.AuthPrincipal;
import com.eventhub.user.dto.UpdateProfileRequest;
import com.eventhub.user.dto.UserResponse;
import com.eventhub.user.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return userService.getById(principal.userId());
    }

    @PatchMapping
    public UserResponse update(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.userId(), request.name());
    }
}
