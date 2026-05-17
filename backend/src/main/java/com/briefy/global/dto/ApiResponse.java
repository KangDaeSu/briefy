package com.briefy.global.dto;

import com.briefy.global.error.BriefyErrorCode;
import org.jspecify.annotations.Nullable;

public record ApiResponse<T>(
        boolean success,
        @Nullable T data,
        @Nullable String message
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(BriefyErrorCode errorCode) {
        return new ApiResponse<>(false, null, errorCode.getMessage());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
