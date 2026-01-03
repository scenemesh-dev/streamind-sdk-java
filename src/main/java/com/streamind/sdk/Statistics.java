package com.streamind.sdk;

/**
 * Connection Statistics
 */
public class Statistics {
    private int signalsSent;
    private int audioSent;
    private int directivesReceived;
    private int audioReceived;
    private int errors;
    private boolean connected;
    private double uptimeSeconds;
    private int reconnectAttempts;

    public Statistics() {
        this.signalsSent = 0;
        this.audioSent = 0;
        this.directivesReceived = 0;
        this.audioReceived = 0;
        this.errors = 0;
        this.connected = false;
        this.uptimeSeconds = 0.0;
        this.reconnectAttempts = 0;
    }

    // Getters and setters
    public int getSignalsSent() { return signalsSent; }
    public void setSignalsSent(int signalsSent) { this.signalsSent = signalsSent; }

    public int getAudioSent() { return audioSent; }
    public void setAudioSent(int audioSent) { this.audioSent = audioSent; }

    public int getDirectivesReceived() { return directivesReceived; }
    public void setDirectivesReceived(int directivesReceived) { this.directivesReceived = directivesReceived; }

    public int getAudioReceived() { return audioReceived; }
    public void setAudioReceived(int audioReceived) { this.audioReceived = audioReceived; }

    public int getErrors() { return errors; }
    public void setErrors(int errors) { this.errors = errors; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public double getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(double uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

    public int getReconnectAttempts() { return reconnectAttempts; }
    public void setReconnectAttempts(int reconnectAttempts) { this.reconnectAttempts = reconnectAttempts; }

    @Override
    public String toString() {
        return String.format(
            "Statistics{signalsSent=%d, audioSent=%d, directivesReceived=%d, " +
            "audioReceived=%d, errors=%d, connected=%b, uptimeSeconds=%.1f, reconnectAttempts=%d}",
            signalsSent, audioSent, directivesReceived, audioReceived,
            errors, connected, uptimeSeconds, reconnectAttempts
        );
    }
}
