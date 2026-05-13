package com.briefy.common.exception;

import com.briefy.common.BriefyErrorCode;
import org.jspecify.annotations.NonNull;

public class BriefyException extends RuntimeException {

    private final BriefyErrorCode errorCode;

    public BriefyException(@NonNull BriefyErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BriefyException(@NonNull BriefyErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    @NonNull
    public BriefyErrorCode getErrorCode() {
        return errorCode;
    }
}
