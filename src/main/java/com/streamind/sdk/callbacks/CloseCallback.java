package com.streamind.sdk.callbacks;

/**
 * Connection close callback
 */
@FunctionalInterface
public interface CloseCallback {
    /**
     * Called when connection is closed
     *
     * @param code close code (1000 = normal, 1006 = abnormal)
     * @param reason close reason
     */
    void onConnectionClosed(int code, String reason);
}
