package com.briefy.domain.user.controller;

import com.briefy.domain.user.dto.UpdateProfileRequest;
import com.briefy.domain.user.dto.UserResponse;
import com.briefy.domain.user.service.UserService;
import com.briefy.global.config.UserPrincipal;
import com.briefy.global.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(UserResponse.from(userService.findById(principal.getUserId())));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(UserResponse.from(userService.updateName(principal.getUserId(), request.name())));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        userService.delete(principal.getUserId());
    }
}
