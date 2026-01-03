package com.streamind.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.streamind.sdk.callbacks.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.*;

/**
 * WebSocket Transport Layer
 *
 * Handles WebSocket connection, heartbeat, and auto-reconnection
 */
public class WebSocketTransport {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketTransport.class);

    private final Config config;
    private WebSocketClient ws;
    private volatile boolean connected = false;
    private volatile boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private long lastActivity = 0;
    private long connectTime = 0;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> reconnectTask;

    // Callbacks
    private ConnectionCallback onConnected;
    private DirectiveCallback onDirective;
    private AudioDataCallback onAudioData;
    private ErrorCallback onError;
    private CloseCallback onClose;

    // Statistics
    private int statsSignalsSent = 0;
    private int statsAudioSent = 0;
    private int statsDirectivesReceived = 0;
    private int statsAudioReceived = 0;
    private int statsErrors = 0;

    public WebSocketTransport(Config config) {
        this.config = config;
    }

    // Callback setters
    public void setConnectionCallback(ConnectionCallback callback) {
        this.onConnected = callback;
    }

    public void setDirectiveCallback(DirectiveCallback callback) {
        this.onDirective = callback;
    }

    public void setAudioDataCallback(AudioDataCallback callback) {
        this.onAudioData = callback;
    }

    public void setErrorCallback(ErrorCallback callback) {
        this.onError = callback;
    }

    public void setCloseCallback(CloseCallback callback) {
        this.onClose = callback;
    }

    /**
     * Connect to WebSocket server
     */
    public void connect(String traceId) throws StreamIndException {
        if (connected) {
            throw new StreamIndException(ErrorCode.ALREADY_CONNECTED);
        }

        shouldReconnect = true;
        String url = config.getWebSocketUrl(traceId != null ? traceId : "");

        try {
            logger.info("Connecting to {}...", config.getEndpoint());

            ws = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    handleOpen();
                }

                @Override
                public void onMessage(String message) {
                    handleTextMessage(message);
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    handleBinaryMessage(bytes.array());
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    handleClose(code, reason);
                }

                @Override
                public void onError(Exception ex) {
                    handleError(ex);
                }
            };

            // Set connection timeout
            ws.setConnectionLostTimeout(config.getConnectionTimeoutMs() / 1000);

            // Connect with timeout
            CompletableFuture<Void> connectFuture = CompletableFuture.runAsync(() -> {
                try {
                    ws.connectBlocking();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            });

            try {
                connectFuture.get(config.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.error("Connection timeout");
                statsErrors++;
                if (onError != null) {
                    onError.onError(ErrorCode.CONNECTION_TIMEOUT, "Connection timeout");
                }
                if (onConnected != null) {
                    onConnected.onConnectionChanged(false, "Connection timeout");
                }
                throw new StreamIndException(ErrorCode.CONNECTION_TIMEOUT);
            } catch (ExecutionException e) {
                throw new StreamIndException(ErrorCode.CONNECTION_FAILED, e.getCause());
            }

        } catch (Exception e) {
            logger.error("Connection failed", e);
            statsErrors++;
            if (onError != null) {
                onError.onError(ErrorCode.CONNECTION_FAILED, e.getMessage());
            }
            if (onConnected != null) {
                onConnected.onConnectionChanged(false, e.getMessage());
            }
            throw new StreamIndException(ErrorCode.CONNECTION_FAILED, e);
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        logger.info("Disconnecting...");
        shouldReconnect = false;

        // Stop tasks
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
        }

        // Close WebSocket
        if (ws != null) {
            ws.close(1000, "Normal disconnection");
            ws = null;
        }

        connected = false;
        logger.info("Disconnected");

        if (onClose != null) {
            onClose.onConnectionClosed(1000, "Normal disconnection");
        }
        if (onConnected != null) {
            onConnected.onConnectionChanged(false, "User disconnected");
        }
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Send signal to platform
     */
    public void sendSignal(Signal signal) throws StreamIndException {
        if (!connected) {
            throw new StreamIndException(ErrorCode.NOT_CONNECTED);
        }

        // Auto-fill source fields
        if (signal.getSource().getReceptorId().isEmpty()) {
            signal.getSource().setReceptorId(config.getDeviceId());
        }
        if (signal.getSource().getReceptorTopic().isEmpty()) {
            signal.getSource().setReceptorTopic(config.getDeviceType());
        }
        if (signal.getSource().getGeneratedTime().isEmpty()) {
            signal.getSource().setGeneratedTime(signal.getTimestamp());
        }

        String jsonStr = signal.toJson();

        // Check message size
        if (jsonStr.getBytes().length > config.getMaxMessageSize()) {
            throw new StreamIndException(ErrorCode.SIGNAL_TOO_LARGE);
        }

        try {
            sendMessage(jsonStr);
            lastActivity = System.currentTimeMillis();
            statsSignalsSent++;
        } catch (Exception e) {
            logger.error("Failed to send signal", e);
            statsErrors++;
            if (onError != null) {
                onError.onError(ErrorCode.SEND_FAILED, e.getMessage());
            }
            throw new StreamIndException(ErrorCode.SEND_FAILED, e);
        }
    }

    /**
     * Send typed binary data to platform with 14-byte application-layer protocol
     *
     * <p><strong>Currently supported data types</strong> (verified by platform):
     * <ul>
     *   <li>"opus" - Audio data in OPUS format</li>
     * </ul>
     *
     * <p>Protocol format (14-byte header):
     * <ul>
     *   <li>Byte 0:      0x82 (protocol identifier)</li>
     *   <li>Byte 1-2:    Data length (big-endian, 2 bytes)</li>
     *   <li>Byte 3-9:    Data type (7-byte ASCII, uppercase, zero-padded)</li>
     *   <li>Byte 10-13:  Mask key (4 random bytes for application-layer XOR masking)</li>
     *   <li>Byte 14+:    XOR-masked actual data</li>
     * </ul>
     *
     * <p>Note: WebSocket library will add standard WebSocket framing/masking automatically.
     *          This 14-byte header is the application-layer protocol, NOT WebSocket framing.
     */
    public void sendBinaryData(byte[] data, String dataType) throws StreamIndException {
        if (!connected) {
            throw new StreamIndException(ErrorCode.NOT_CONNECTED);
        }

        try {
            // Build application-layer protocol data (same as hardware SDK)
            ByteBuffer protocolData = ByteBuffer.allocate(14 + data.length);

            // Byte 0: 0x82 (application layer protocol identifier)
            protocolData.put((byte) 0x82);

            // Byte 1-2: Data length (big-endian, 2 bytes)
            int dataLen = data.length;
            if (dataLen > 65535) {
                throw new StreamIndException(ErrorCode.SIGNAL_TOO_LARGE, "Binary data exceeds 65535 bytes");
            }

            protocolData.put((byte) ((dataLen >> 8) & 0xFF));  // High byte
            protocolData.put((byte) (dataLen & 0xFF));         // Low byte

            // Byte 3-9: Data type (7-byte ASCII, uppercase, padded with 0x00)
            String dataTypeStr = dataType.toUpperCase();
            if (dataTypeStr.length() > 7) {
                dataTypeStr = dataTypeStr.substring(0, 7);
            }
            byte[] dataTypeBytes = new byte[7];
            byte[] typeBytes = dataTypeStr.getBytes("ASCII");
            System.arraycopy(typeBytes, 0, dataTypeBytes, 0, Math.min(typeBytes.length, 7));
            protocolData.put(dataTypeBytes);

            // Byte 10-13: Mask key (4 random bytes) - application layer masking
            byte[] maskKey = new byte[4];
            new Random().nextBytes(maskKey);
            protocolData.put(maskKey);

            // Byte 14+: XOR-masked actual data (application layer masking)
            // This is part of the application protocol, NOT WebSocket masking
            for (int i = 0; i < data.length; i++) {
                protocolData.put((byte) (data[i] ^ maskKey[i % 4]));
            }

            // Send through WebSocket (library will add WebSocket framing/masking automatically)
            sendBinary(protocolData.array());

            lastActivity = System.currentTimeMillis();
            statsAudioSent++;  // Keep using audioSent for backward compatibility

        } catch (Exception e) {
            logger.error("Failed to send binary data", e);
            statsErrors++;
            if (onError != null) {
                onError.onError(ErrorCode.SEND_FAILED, e.getMessage());
            }
            throw new StreamIndException(ErrorCode.SEND_FAILED, e);
        }
    }

    /**
     * Convenience method: Send audio data to platform
     */
    public void sendAudioData(byte[] data, String audioFormat) throws StreamIndException {
        sendBinaryData(data, audioFormat);
    }

    /**
     * Get statistics
     */
    public Statistics getStatistics() {
        Statistics stats = new Statistics();
        stats.setSignalsSent(statsSignalsSent);
        stats.setAudioSent(statsAudioSent);
        stats.setDirectivesReceived(statsDirectivesReceived);
        stats.setAudioReceived(statsAudioReceived);
        stats.setErrors(statsErrors);
        stats.setConnected(connected);
        stats.setReconnectAttempts(reconnectAttempts);

        if (connected && connectTime > 0) {
            stats.setUptimeSeconds((System.currentTimeMillis() - connectTime) / 1000.0);
        } else {
            stats.setUptimeSeconds(0.0);
        }

        return stats;
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        statsSignalsSent = 0;
        statsAudioSent = 0;
        statsDirectivesReceived = 0;
        statsAudioReceived = 0;
        statsErrors = 0;
    }

    /**
     * Shutdown the transport (cleanup resources)
     */
    public void shutdown() {
        disconnect();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Private methods

    private void handleOpen() {
        // Enable TCP_NODELAY for real-time performance
        try {
            Socket socket = ws.getSocket();
            if (socket != null) {
                socket.setTcpNoDelay(true);
                logger.debug("TCP_NODELAY enabled for real-time performance");
            }
        } catch (Exception e) {
            logger.warn("Failed to set TCP_NODELAY", e);
        }

        connected = true;
        reconnectAttempts = 0;
        lastActivity = System.currentTimeMillis();
        connectTime = System.currentTimeMillis();

        logger.info("Connected to platform");
        if (onConnected != null) {
            onConnected.onConnectionChanged(true, "");
        }

        // Start heartbeat
        startHeartbeat();
    }

    private void handleTextMessage(String message) {
        lastActivity = System.currentTimeMillis();

        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            // Check if it's a directive
            if (json.has("name") && json.has("id")) {
                Directive directive = Directive.fromJson(message);
                statsDirectivesReceived++;
                if (onDirective != null && config.isEnableDirectiveReceiving()) {
                    onDirective.onDirectiveReceived(directive);
                }
            } else {
                logger.debug("Received message: {}", json);
            }

        } catch (Exception e) {
            logger.warn("Invalid JSON message: {}", message);
            statsErrors++;
            if (onError != null) {
                onError.onError(ErrorCode.INTERNAL_ERROR, "Invalid JSON message");
            }
        }
    }

    private void handleBinaryMessage(byte[] data) {
        lastActivity = System.currentTimeMillis();
        statsAudioReceived++;
        if (onAudioData != null) {
            onAudioData.onAudioDataReceived(data);
        }
    }

    private void handleClose(int code, String reason) {
        logger.warn("Connection closed: {} - {}", code, reason);
        if (onClose != null) {
            onClose.onConnectionClosed(code, reason != null ? reason : "Abnormal closure");
        }
        handleDisconnect();
    }

    private void handleError(Exception ex) {
        logger.error("WebSocket error", ex);
        statsErrors++;
        if (onError != null) {
            onError.onError(ErrorCode.CONNECTION_FAILED, ex.getMessage());
        }
        if (!connected && onConnected != null) {
            onConnected.onConnectionChanged(false, ex.getMessage());
        }
    }

    private void sendMessage(String message) {
        if (ws != null && connected) {
            ws.send(message);
        }
    }

    private void sendBinary(byte[] data) {
        if (ws != null && connected) {
            ws.send(data);
        }
    }

    private void startHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }

        long intervalMs = config.getHeartbeatIntervalMs();
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (!connected) {
                return;
            }

            // Only send heartbeat if no recent activity
            long now = System.currentTimeMillis();
            if (now - lastActivity >= intervalMs) {
                try {
                    String heartbeat = "{\"type\":\"ping\"}";
                    sendMessage(heartbeat);
                    logger.debug("Heartbeat sent");
                } catch (Exception e) {
                    logger.error("Heartbeat failed", e);
                    handleDisconnect();
                }
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void handleDisconnect() {
        if (!connected) {
            return;
        }

        connected = false;
        ws = null;

        // Stop heartbeat
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }

        logger.warn("Disconnected from platform");
        if (onConnected != null) {
            onConnected.onConnectionChanged(false, "Connection lost");
        }

        // Trigger auto-reconnect
        if (shouldReconnect) {
            startReconnect();
        }
    }

    private void startReconnect() {
        // Check max attempts
        if (config.getMaxReconnectAttempts() > 0 &&
            reconnectAttempts >= config.getMaxReconnectAttempts()) {
            logger.error("Max reconnect attempts reached");
            return;
        }

        // Calculate backoff delay
        long delay = calculateBackoffDelay();
        logger.info("Reconnecting in {} seconds (attempt {})...",
                    delay / 1000.0, reconnectAttempts + 1);

        reconnectTask = scheduler.schedule(() -> {
            if (!shouldReconnect || connected) {
                return;
            }

            reconnectAttempts++;
            try {
                connect("");
            } catch (Exception e) {
                logger.error("Reconnect failed", e);
                startReconnect();  // Try again
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private long calculateBackoffDelay() {
        long base = config.getBaseReconnectIntervalMs();
        long maxDelay = config.getMaxReconnectIntervalMs();
        double factor = config.getBackoffFactor();
        double jitter = config.getJitterFactor();

        // Exponential backoff
        double delay = base * Math.pow(factor, reconnectAttempts);
        delay = Math.min(delay, maxDelay);

        // Add jitter
        double jitterAmount = delay * jitter * (new Random().nextDouble() * 2 - 1);
        delay += jitterAmount;

        return Math.max(0, (long) delay);
    }
}
