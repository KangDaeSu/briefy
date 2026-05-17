package com.briefy.domain.user.service;

import com.briefy.domain.user.dto.AuthResult;
import com.briefy.domain.user.entity.User;
import com.briefy.domain.user.repository.UserRepository;
import com.briefy.global.config.JwtProvider;
import com.briefy.global.error.BriefyErrorCode;
import com.briefy.global.error.BriefyException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthResult register(@NonNull String email, @NonNull String name, @NonNull String password) {
        if (userRepository.existsByEmail(email)) {
            throw new BriefyException(BriefyErrorCode.USER_ALREADY_EXISTS);
        }
        User user = new User(email, name);
        user.updatePasswordHash(passwordEncoder.encode(password));
        user = userRepository.save(user);
        return new AuthResult(user, jwtProvider.generate(user.getId(), user.getEmail()));
    }

    public AuthResult login(@NonNull String email, @NonNull String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BriefyException(BriefyErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BriefyException(BriefyErrorCode.INVALID_CREDENTIALS);
        }
        return new AuthResult(user, jwtProvider.generate(user.getId(), user.getEmail()));
    }
}
