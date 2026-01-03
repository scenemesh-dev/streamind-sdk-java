package com.streamind.sdk;

/**
 * SDK Configuration
 */
public class Config {
    private final String deviceId;
    private final String deviceType;
    private final String endpoint;
    private final String tenantId;
    private final String productId;
    private final String productKey;
    private final boolean enableDirectiveReceiving;
    private final int connectionTimeoutMs;
    private final int heartbeatIntervalMs;
    private final int maxMessageSize;
    private final int maxReconnectAttempts;
    private final int baseReconnectIntervalMs;
    private final int maxReconnectIntervalMs;
    private final double backoffFactor;
    private final double jitterFactor;

    private Config(Builder builder) {
        this.deviceId = builder.deviceId;
        this.deviceType = builder.deviceType;
        this.endpoint = builder.endpoint;
        this.tenantId = builder.tenantId;
        this.productId = builder.productId;
        this.productKey = builder.productKey;
        this.enableDirectiveReceiving = builder.enableDirectiveReceiving;
        this.connectionTimeoutMs = builder.connectionTimeoutMs;
        this.heartbeatIntervalMs = builder.heartbeatIntervalMs;
        this.maxMessageSize = builder.maxMessageSize;
        this.maxReconnectAttempts = builder.maxReconnectAttempts;
        this.baseReconnectIntervalMs = builder.baseReconnectIntervalMs;
        this.maxReconnectIntervalMs = builder.maxReconnectIntervalMs;
        this.backoffFactor = builder.backoffFactor;
        this.jitterFactor = builder.jitterFactor;
    }

    // Getters
    public String getDeviceId() { return deviceId; }
    public String getDeviceType() { return deviceType; }
    public String getEndpoint() { return endpoint; }
    public String getTenantId() { return tenantId; }
    public String getProductId() { return productId; }
    public String getProductKey() { return productKey; }
    public boolean isEnableDirectiveReceiving() { return enableDirectiveReceiving; }
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public int getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
    public int getMaxMessageSize() { return maxMessageSize; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public int getBaseReconnectIntervalMs() { return baseReconnectIntervalMs; }
    public int getMaxReconnectIntervalMs() { return maxReconnectIntervalMs; }
    public double getBackoffFactor() { return backoffFactor; }
    public double getJitterFactor() { return jitterFactor; }

    /**
     * Get WebSocket URL with query parameters
     */
    public String getWebSocketUrl(String traceId) {
        StringBuilder url = new StringBuilder(endpoint);
        url.append("?tenantId=").append(tenantId);
        url.append("&productId=").append(productId);
        url.append("&productKey=").append(productKey);
        if (traceId != null && !traceId.isEmpty()) {
            url.append("&traceId=").append(traceId);
        }
        return url.toString();
    }

    /**
     * Builder class for Config
     */
    public static class Builder {
        // Required parameters
        private final String deviceId;
        private final String deviceType;
        private final String endpoint;
        private final String tenantId;
        private final String productId;
        private final String productKey;

        // Optional parameters with defaults
        private boolean enableDirectiveReceiving = true;
        private int connectionTimeoutMs = 10000;
        private int heartbeatIntervalMs = 5000;
        private int maxMessageSize = 10 * 1024 * 1024;
        private int maxReconnectAttempts = -1;
        private int baseReconnectIntervalMs = 1000;
        private int maxReconnectIntervalMs = 60000;
        private double backoffFactor = 2.0;
        private double jitterFactor = 0.1;

        public Builder(String deviceId, String deviceType, String endpoint,
                      String tenantId, String productId, String productKey) {
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.endpoint = endpoint;
            this.tenantId = tenantId;
            this.productId = productId;
            this.productKey = productKey;
        }

        public Builder enableDirectiveReceiving(boolean enable) {
            this.enableDirectiveReceiving = enable;
            return this;
        }

        public Builder connectionTimeoutMs(int timeout) {
            this.connectionTimeoutMs = timeout;
            return this;
        }

        public Builder heartbeatIntervalMs(int interval) {
            this.heartbeatIntervalMs = interval;
            return this;
        }

        public Builder maxMessageSize(int size) {
            this.maxMessageSize = size;
            return this;
        }

        public Builder maxReconnectAttempts(int attempts) {
            this.maxReconnectAttempts = attempts;
            return this;
        }

        public Builder baseReconnectIntervalMs(int interval) {
            this.baseReconnectIntervalMs = interval;
            return this;
        }

        public Builder maxReconnectIntervalMs(int interval) {
            this.maxReconnectIntervalMs = interval;
            return this;
        }

        public Builder backoffFactor(double factor) {
            this.backoffFactor = factor;
            return this;
        }

        public Builder jitterFactor(double factor) {
            this.jitterFactor = factor;
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
