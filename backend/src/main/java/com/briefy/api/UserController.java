package com.briefy.api;

import com.briefy.api.dto.UpdateProfileRequest;
import com.briefy.api.dto.UserResponse;
import com.briefy.common.ApiResponse;
import com.briefy.domain.user.UserService;
import com.briefy.infra.security.UserPrincipal;
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
