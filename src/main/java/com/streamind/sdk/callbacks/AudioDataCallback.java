package com.streamind.sdk.callbacks;

/**
 * Audio data received callback
 */
@FunctionalInterface
public interface AudioDataCallback {
    /**
     * Called when audio data is received from platform
     *
     * @param data the received audio data
     */
    void onAudioDataReceived(byte[] data);
}
