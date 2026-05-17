package com.briefy.infra.security;

import com.briefy.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final String frontendUrl;

    public OAuth2SuccessHandler(JwtProvider jwtProvider,
                                UserRepository userRepository,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.sendRedirect(frontendUrl + "/login?error=oauth_user_not_found");
            return;
        }

        var user = userOpt.get();
        String token = jwtProvider.generate(user.getId(), user.getEmail());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token).toString());
        response.sendRedirect(frontendUrl + "/");
    }

    private ResponseCookie buildCookie(String token) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
    }
}
