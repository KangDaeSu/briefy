package com.briefy.domain.auth;

import com.briefy.common.BriefyErrorCode;
import com.briefy.common.exception.BriefyException;
import com.briefy.domain.user.AuthProvider;
import com.briefy.domain.user.User;
import com.briefy.domain.user.UserRepository;
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
    @InjectMocks AuthService authService;

    @Test
    void register_success() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("$2a$hashed");

        User saved = new User("alice@test.com", "Alice", AuthProvider.LOCAL, null);
        saved.updatePasswordHash("$2a$hashed");
        when(userRepository.save(any())).thenReturn(saved);

        User result = authService.register("alice@test.com", "Alice", "pass1234");

        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        assertThat(result.getPasswordHash()).isEqualTo("$2a$hashed");
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
        User user = new User("bob@test.com", "Bob", AuthProvider.LOCAL, null);
        user.updatePasswordHash("$2a$hashed");

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass1234", "$2a$hashed")).thenReturn(true);

        User result = authService.login("bob@test.com", "pass1234");

        assertThat(result.getEmail()).isEqualTo("bob@test.com");
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
        User user = new User("carol@test.com", "Carol", AuthProvider.LOCAL, null);
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
        // Google OAuth 계정은 password_hash가 null — 이메일 로그인 불가
        User googleUser = new User("google@test.com", "Google User", AuthProvider.GOOGLE, "google-sub-123");

        when(userRepository.findByEmail("google@test.com")).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authService.login("google@test.com", "anypass"))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.INVALID_CREDENTIALS));
    }
}
