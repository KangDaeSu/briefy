package com.briefy.infra.security;

import com.briefy.domain.user.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    @Nullable
    private final String password;

    private UserPrincipal(UUID userId, String email, @Nullable String password) {
        this.userId = userId;
        this.email = email;
        this.password = password;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    public static UserPrincipal fromJwt(UUID userId, String email) {
        return new UserPrincipal(userId, email, null);
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    @Nullable
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
