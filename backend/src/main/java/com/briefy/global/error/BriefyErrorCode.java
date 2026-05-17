package com.briefy.global.error;

public enum BriefyErrorCode {

    // Schedule
    SCHEDULE_NOT_FOUND("S001", "일정을 찾을 수 없습니다"),
    SCHEDULE_OVERLAP("S002", "해당 시간에 이미 일정이 있습니다"),
    SCHEDULE_INVALID_TIME("S003", "종료 시간은 시작 시간 이후여야 합니다"),

    // User
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다"),

    // Auth
    UNAUTHORIZED("A001", "인증이 필요합니다"),
    FORBIDDEN("A002", "접근 권한이 없습니다"),
    INVALID_TOKEN("A003", "유효하지 않은 토큰입니다"),
    INVALID_CREDENTIALS("A004", "이메일 또는 비밀번호가 올바르지 않습니다");

    private final String code;
    private final String message;

    BriefyErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
