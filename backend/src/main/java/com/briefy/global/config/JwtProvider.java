package com.briefy.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtProvider(JwtProperties props) {
        String s = props.secret();
        System.out.println(">>> JWT secret length = " + (s == null ? "NULL" : s.length()));
        this.secretKey = Keys.hmacShaKeyFor(sha256(props.secret().strip()));
        this.expirationMs = props.expirationMs();
    }

    public String generate(UUID userId, String email) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Optional<Claims> tryParse(String token) {
        try {
            return Optional.of(parse(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isValid(String token) {
        return tryParse(token).isPresent();
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
