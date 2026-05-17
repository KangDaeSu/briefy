package com.briefy.api;

import com.briefy.api.dto.LoginRequest;
import com.briefy.api.dto.RegisterRequest;
import com.briefy.api.dto.UserResponse;
import com.briefy.common.ApiResponse;
import com.briefy.common.BriefyErrorCode;
import com.briefy.common.exception.BriefyException;
import com.briefy.domain.auth.AuthService;
import com.briefy.domain.user.User;
import com.briefy.domain.user.UserService;
import com.briefy.infra.security.JwtProvider;
import com.briefy.infra.security.UserPrincipal;
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
    private final JwtProvider jwtProvider;
    private final UserService userService;

    public AuthController(AuthService authService, JwtProvider jwtProvider, UserService userService) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                              HttpServletResponse response) {
        User user = authService.register(request.email(), request.name(), request.password());
        issueToken(response, user);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletResponse response) {
        User user = authService.login(request.email(), request.password());
        issueToken(response, user);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("jwt", "")
                        .httpOnly(true)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build()
                        .toString());
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BriefyException(BriefyErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(UserResponse.from(userService.findById(principal.getUserId())));
    }

    private void issueToken(HttpServletResponse response, User user) {
        String token = jwtProvider.generate(user.getId(), user.getEmail());
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("jwt", token)
                        .httpOnly(true)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofDays(7))
                        .build()
                        .toString());
    }
}
