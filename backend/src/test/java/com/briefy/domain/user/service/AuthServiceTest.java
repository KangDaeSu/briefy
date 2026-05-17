package com.briefy.domain.user.service;

import com.briefy.domain.user.dto.AuthResult;
import com.briefy.domain.user.entity.User;
import com.briefy.domain.user.repository.UserRepository;
import com.briefy.global.config.JwtProvider;
import com.briefy.global.error.BriefyErrorCode;
import com.briefy.global.error.BriefyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;
    @InjectMocks AuthService authService;

    @Test
    void register_success() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("$2a$hashed");
        when(jwtProvider.generate(any(), any())).thenReturn("token");

        User saved = new User("alice@test.com", "Alice");
        saved.updatePasswordHash("$2a$hashed");
        when(userRepository.save(any())).thenReturn(saved);

        AuthResult result = authService.register("alice@test.com", "Alice", "pass1234");

        assertThat(result.user().getEmail()).isEqualTo("alice@test.com");
        assertThat(result.user().getPasswordHash()).isEqualTo("$2a$hashed");
        assertThat(result.token()).isEqualTo("token");
        verify(passwordEncoder).encode("pass1234");
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("dup@test.com", "Alice", "pass1234"))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.USER_ALREADY_EXISTS));
    }

    @Test
    void login_success() {
        User user = new User("bob@test.com", "Bob");
        user.updatePasswordHash("$2a$hashed");
        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass1234", "$2a$hashed")).thenReturn(true);
        when(jwtProvider.generate(any(), any())).thenReturn("token");

        AuthResult result = authService.login("bob@test.com", "pass1234");

        assertThat(result.user().getEmail()).isEqualTo("bob@test.com");
        assertThat(result.token()).isEqualTo("token");
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@test.com", "pass"))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User("carol@test.com", "Carol");
        user.updatePasswordHash("$2a$hashed");
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("carol@test.com", "wrong"))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_noPasswordHash_throws() {
        User user = new User("nopass@test.com", "No Pass");
        when(userRepository.findByEmail("nopass@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("nopass@test.com", "anypass"))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.INVALID_CREDENTIALS));
    }
}
