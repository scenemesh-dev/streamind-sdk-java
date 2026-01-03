package com.streamind.sdk.callbacks;

/**
 * Connection state callback
 */
@FunctionalInterface
public interface ConnectionCallback {
    /**
     * Called when connection state changes
     *
     * @param connected true if connected, false if disconnected
     * @param errorMessage error message if disconnected, empty if connected
     */
    void onConnectionChanged(boolean connected, String errorMessage);
}
