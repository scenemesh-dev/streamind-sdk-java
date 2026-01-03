package com.streamind.sdk;

/**
 * StreamInd SDK Error Codes
 */
public enum ErrorCode {
    OK(0, "Success"),
    NOT_INITIALIZED(1, "SDK not initialized"),
    ALREADY_INITIALIZED(2, "SDK already initialized"),
    INVALID_CONFIG(3, "Invalid configuration"),
    NOT_CONNECTED(4, "Not connected to platform"),
    ALREADY_CONNECTED(5, "Already connected to platform"),
    CONNECTION_FAILED(6, "Connection failed"),
    CONNECTION_TIMEOUT(7, "Connection timeout"),
    INVALID_SIGNAL(8, "Invalid signal"),
    SIGNAL_TOO_LARGE(9, "Signal exceeds maximum size"),
    SEND_FAILED(10, "Send failed"),
    INVALID_PARAMETER(11, "Invalid parameter"),
    TERMINAL_NOT_FOUND(12, "Terminal not found"),
    INTERNAL_ERROR(99, "Internal error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
