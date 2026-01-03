package com.streamind.sdk.callbacks;

import com.streamind.sdk.ErrorCode;

/**
 * Error callback
 */
@FunctionalInterface
public interface ErrorCallback {
    /**
     * Called when an error occurs
     *
     * @param errorCode the error code
     * @param message error message
     */
    void onError(ErrorCode errorCode, String message);
}
