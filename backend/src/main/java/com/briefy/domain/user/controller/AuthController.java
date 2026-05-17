package com.briefy.domain.user.controller;

import com.briefy.domain.user.dto.LoginRequest;
import com.briefy.domain.user.dto.RegisterRequest;
import com.briefy.domain.user.dto.UserResponse;
import com.briefy.domain.user.dto.AuthResult;
import com.briefy.domain.user.service.AuthService;
import com.briefy.domain.user.service.UserService;
import com.briefy.global.config.UserPrincipal;
import com.briefy.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                              HttpServletResponse response) {
        AuthResult result = authService.register(request.email(), request.name(), request.password());
        setJwtCookie(response, result.token());
        return ApiResponse.ok(UserResponse.from(result.user()));
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletResponse response) {
        AuthResult result = authService.login(request.email(), request.password());
        setJwtCookie(response, result.token());
        return ApiResponse.ok(UserResponse.from(result.user()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        clearJwtCookie(response);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(UserResponse.from(userService.findById(principal.getUserId())));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("jwt", token)
                        .httpOnly(true)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofDays(7))
                        .build()
                        .toString());
    }

    private void clearJwtCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("jwt", "")
                        .httpOnly(true)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build()
                        .toString());
    }
}
