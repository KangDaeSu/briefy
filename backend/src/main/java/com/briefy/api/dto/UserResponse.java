package com.briefy.api.dto;

import com.briefy.domain.user.AuthProvider;
import com.briefy.domain.user.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        AuthProvider provider
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getProvider());
    }
}
