package com.streamind.sdk;

/**
 * StreamInd SDK Exception
 */
public class StreamIndException extends Exception {
    private final ErrorCode errorCode;

    public StreamIndException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public StreamIndException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StreamIndException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public StreamIndException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
