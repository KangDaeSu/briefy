package com.briefy.domain.user.service;

import com.briefy.domain.user.entity.PasswordResetToken;
import com.briefy.domain.user.entity.User;
import com.briefy.domain.user.repository.PasswordResetTokenRepository;
import com.briefy.domain.user.repository.UserRepository;
import com.briefy.global.error.BriefyErrorCode;
import com.briefy.global.error.BriefyException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
@ConditionalOnBean(JavaMailSender.class)
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final int EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String fromEmail;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                JavaMailSender mailSender,
                                @Value("${app.frontend-url}") String frontendUrl,
                                @Value("${app.mail.from}") String fromEmail) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.fromEmail = fromEmail;
    }

    @Transactional
    public void requestReset(@NonNull String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.deleteAllByUser(user);

            String rawToken = generateRawToken();
            String tokenHash = sha256(rawToken);
            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(EXPIRY_MINUTES);

            tokenRepository.save(new PasswordResetToken(user, tokenHash, expiresAt));
            sendResetEmail(user, rawToken);
        });
        // 이메일이 존재하지 않아도 조용히 성공 — 계정 존재 여부 열거 방지
    }

    @Transactional
    public void resetPassword(@NonNull String rawToken, @NonNull String newPassword) {
        String hash = sha256(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BriefyException(BriefyErrorCode.TOKEN_INVALID));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new BriefyException(BriefyErrorCode.TOKEN_INVALID);
        }

        User user = token.getUser();
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        tokenRepository.delete(token);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(@NonNull String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void sendResetEmail(@NonNull User user, @NonNull String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setFrom(fromEmail);
        message.setSubject("[briefy] 비밀번호 재설정");
        message.setText("""
                안녕하세요, %s님.

                비밀번호 재설정을 요청하셨습니다.
                아래 링크를 클릭해 새 비밀번호를 설정하세요 (유효 시간: 30분).

                %s/reset-password?token=%s

                본인이 요청하지 않으셨다면 이 메일을 무시해 주세요.
                """.formatted(user.getName(), frontendUrl, rawToken));
        mailSender.send(message);
    }
}
