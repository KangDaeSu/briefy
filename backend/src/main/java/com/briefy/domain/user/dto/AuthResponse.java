package com.briefy.domain.user.dto;

public record AuthResponse(UserResponse user, String token) {}
