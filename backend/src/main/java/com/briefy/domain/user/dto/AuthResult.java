package com.briefy.domain.user.dto;

import com.briefy.domain.user.entity.User;

public record AuthResult(User user, String token) {}
