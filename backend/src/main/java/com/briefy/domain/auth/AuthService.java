package com.briefy.domain.auth;

import com.briefy.common.BriefyErrorCode;
import com.briefy.common.exception.BriefyException;
import com.briefy.domain.user.AuthProvider;
import com.briefy.domain.user.User;
import com.briefy.domain.user.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(@NonNull String email, @NonNull String name, @NonNull String password) {
        if (userRepository.existsByEmail(email)) {
            throw new BriefyException(BriefyErrorCode.USER_ALREADY_EXISTS);
        }
        User user = new User(email, name, AuthProvider.LOCAL, null);
        user.updatePasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User login(@NonNull String email, @NonNull String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BriefyException(BriefyErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BriefyException(BriefyErrorCode.INVALID_CREDENTIALS);
        }
        return user;
    }
}
