package com.streamind.sdk;

import com.streamind.sdk.callbacks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * StreamInd SDK Main Class
 *
 * Multi-terminal management with high-performance concurrent operations
 */
public class StreamIndSDK {
    private static final Logger logger = LoggerFactory.getLogger(StreamIndSDK.class);
    private static final String VERSION = "1.0.0";

    private final Map<String, Terminal> terminals = new ConcurrentHashMap<>();
    private String lastError = "";

    // Global callbacks
    private GlobalConnectionCallback globalConnectionCallback;
    private GlobalDirectiveCallback globalDirectiveCallback;
    private GlobalErrorCallback globalErrorCallback;
    private GlobalCloseCallback globalCloseCallback;

    /**
     * Terminal instance
     */
    private static class Terminal {
        final Config config;
        final WebSocketTransport transport;

        Terminal(Config config, WebSocketTransport transport) {
            this.config = config;
            this.transport = transport;
        }
    }

    /**
     * Global callback interfaces
     */
    @FunctionalInterface
    public interface GlobalConnectionCallback {
        void onConnectionChanged(String terminalId, boolean connected, String errorMessage);
    }

    @FunctionalInterface
    public interface GlobalDirectiveCallback {
        void onDirectiveReceived(String terminalId, Directive directive);
    }

    @FunctionalInterface
    public interface GlobalErrorCallback {
        void onError(String terminalId, ErrorCode errorCode, String message);
    }

    @FunctionalInterface
    public interface GlobalCloseCallback {
        void onConnectionClosed(String terminalId, int code, String reason);
    }

    /**
     * Get SDK version
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Register a terminal
     */
    public ErrorCode registerTerminal(String terminalId, Config config) {
        if (terminals.containsKey(terminalId)) {
            lastError = "Terminal " + terminalId + " already registered";
            return ErrorCode.ALREADY_INITIALIZED;
        }

        WebSocketTransport transport = new WebSocketTransport(config);

        // Apply global callbacks if set
        if (globalConnectionCallback != null) {
            transport.setConnectionCallback((connected, errorMessage) ->
                globalConnectionCallback.onConnectionChanged(terminalId, connected, errorMessage));
        }

        if (globalDirectiveCallback != null) {
            transport.setDirectiveCallback(directive ->
                globalDirectiveCallback.onDirectiveReceived(terminalId, directive));
        }

        if (globalErrorCallback != null) {
            transport.setErrorCallback((errorCode, message) ->
                globalErrorCallback.onError(terminalId, errorCode, message));
        }

        if (globalCloseCallback != null) {
            transport.setCloseCallback((code, reason) ->
                globalCloseCallback.onConnectionClosed(terminalId, code, reason));
        }

        terminals.put(terminalId, new Terminal(config, transport));
        return ErrorCode.OK;
    }

    /**
     * Unregister a terminal
     */
    public ErrorCode unregisterTerminal(String terminalId) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        // Disconnect if connected
        if (terminal.transport.isConnected()) {
            terminal.transport.disconnect();
        }

        terminal.transport.shutdown();
        terminals.remove(terminalId);
        return ErrorCode.OK;
    }

    /**
     * Get all registered terminal IDs
     */
    public List<String> getAllTerminals() {
        return new ArrayList<>(terminals.keySet());
    }

    /**
     * Get all connected terminal IDs
     */
    public List<String> getConnectedTerminals() {
        return terminals.entrySet().stream()
            .filter(entry -> entry.getValue().transport.isConnected())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Connect a terminal to platform
     */
    public ErrorCode connect(String terminalId, String traceId) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        try {
            terminal.transport.connect(traceId);
            return ErrorCode.OK;
        } catch (StreamIndException e) {
            lastError = e.getMessage();
            return e.getErrorCode();
        }
    }

    /**
     * Connect a terminal to platform (without trace ID)
     */
    public ErrorCode connect(String terminalId) {
        return connect(terminalId, "");
    }

    /**
     * Connect all registered terminals concurrently
     */
    public Map<String, ErrorCode> connectAll() {
        Map<String, ErrorCode> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String terminalId : terminals.keySet()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                ErrorCode code = connect(terminalId);
                results.put(terminalId, code);
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return results;
    }

    /**
     * Disconnect a terminal
     */
    public ErrorCode disconnect(String terminalId) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        try {
            terminal.transport.disconnect();
            return ErrorCode.OK;
        } catch (Exception e) {
            lastError = e.getMessage();
            return ErrorCode.INTERNAL_ERROR;
        }
    }

    /**
     * Disconnect all connected terminals concurrently
     */
    public Map<String, ErrorCode> disconnectAll() {
        Map<String, ErrorCode> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String terminalId : terminals.keySet()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                ErrorCode code = disconnect(terminalId);
                results.put(terminalId, code);
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return results;
    }

    /**
     * Check if a terminal is connected
     */
    public boolean isConnected(String terminalId) {
        Terminal terminal = terminals.get(terminalId);
        return terminal != null && terminal.transport.isConnected();
    }

    /**
     * Send signal through a terminal
     */
    public ErrorCode sendSignal(String terminalId, Signal signal) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        try {
            terminal.transport.sendSignal(signal);
            return ErrorCode.OK;
        } catch (StreamIndException e) {
            lastError = e.getMessage();
            return e.getErrorCode();
        }
    }

    /**
     * Send multiple signals concurrently (batch)
     */
    public Map<Integer, ErrorCode> sendSignalsBatch(String terminalId, List<Signal> signals) {
        Map<Integer, ErrorCode> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < signals.size(); i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                ErrorCode code = sendSignal(terminalId, signals.get(index));
                results.put(index, code);
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return results;
    }

    /**
     * Send typed binary data through a terminal
     *
     * <p>Currently verified data types: "opus" (audio)
     *
     * @param terminalId Terminal identifier
     * @param data Binary data bytes
     * @param dataType Data type identifier (currently only "opus" is verified by platform)
     * @return ErrorCode.OK on success, error code otherwise
     */
    public ErrorCode sendBinaryData(String terminalId, byte[] data, String dataType) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        try {
            terminal.transport.sendBinaryData(data, dataType);
            return ErrorCode.OK;
        } catch (StreamIndException e) {
            lastError = e.getMessage();
            return e.getErrorCode();
        }
    }

    /**
     * Convenience method: Send audio data through a terminal
     *
     * @param terminalId Terminal identifier
     * @param data Audio data bytes
     * @param audioFormat Audio format (default: "opus" - currently the only verified format)
     * @return ErrorCode.OK on success, error code otherwise
     */
    public ErrorCode sendAudioData(String terminalId, byte[] data, String audioFormat) {
        return sendBinaryData(terminalId, data, audioFormat);
    }

    /**
     * Convenience method: Send audio data through a terminal (default format: opus)
     *
     * @param terminalId Terminal identifier
     * @param data Audio data bytes
     * @return ErrorCode.OK on success, error code otherwise
     */
    public ErrorCode sendAudioData(String terminalId, byte[] data) {
        return sendBinaryData(terminalId, data, "opus");
    }

    /**
     * Convenience method: Send text signal
     */
    public ErrorCode sendText(String terminalId, String signalType, String text, Map<String, Object> extra) {
        Signal signal = new Signal(signalType);
        signal.getPayload().setString("text", text);

        if (extra != null) {
            for (Map.Entry<String, Object> entry : extra.entrySet()) {
                signal.getPayload().setObject(entry.getKey(), entry.getValue());
            }
        }

        return sendSignal(terminalId, signal);
    }

    /**
     * Convenience method: Send text signal (without extra data)
     */
    public ErrorCode sendText(String terminalId, String signalType, String text) {
        return sendText(terminalId, signalType, text, null);
    }

    /**
     * Convenience method: Send JSON signal
     */
    public ErrorCode sendJSON(String terminalId, String signalType, Map<String, Object> data) {
        Signal signal = new Signal(signalType);
        signal.getPayload().setData(data);
        return sendSignal(terminalId, signal);
    }

    /**
     * Set connection callback for a terminal
     */
    public ErrorCode setConnectionCallback(String terminalId, ConnectionCallback callback) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        terminal.transport.setConnectionCallback(callback);
        return ErrorCode.OK;
    }

    /**
     * Set directive callback for a terminal
     */
    public ErrorCode setDirectiveCallback(String terminalId, DirectiveCallback callback) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        terminal.transport.setDirectiveCallback(callback);
        return ErrorCode.OK;
    }

    /**
     * Set audio data callback for a terminal
     */
    public ErrorCode setAudioDataCallback(String terminalId, AudioDataCallback callback) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        terminal.transport.setAudioDataCallback(callback);
        return ErrorCode.OK;
    }

    /**
     * Set error callback for a terminal
     */
    public ErrorCode setErrorCallback(String terminalId, ErrorCallback callback) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        terminal.transport.setErrorCallback(callback);
        return ErrorCode.OK;
    }

    /**
     * Set close callback for a terminal
     */
    public ErrorCode setCloseCallback(String terminalId, CloseCallback callback) {
        Terminal terminal = terminals.get(terminalId);
        if (terminal == null) {
            lastError = "Terminal " + terminalId + " not found";
            return ErrorCode.TERMINAL_NOT_FOUND;
        }

        terminal.transport.setCloseCallback(callback);
        return ErrorCode.OK;
    }

    /**
     * Set global connection callback (applies to all terminals)
     */
    public void setGlobalConnectionCallback(GlobalConnectionCallback callback) {
        this.globalConnectionCallback = callback;

        // Apply to existing terminals
        for (Map.Entry<String, Terminal> entry : terminals.entrySet()) {
            String terminalId = entry.getKey();
            entry.getValue().transport.setConnectionCallback((connected, errorMessage) ->
                callback.onConnectionChanged(terminalId, connected, errorMessage));
        }
    }

    /**
     * Set global directive callback (applies to all terminals)
     */
    public void setGlobalDirectiveCallback(GlobalDirectiveCallback callback) {
        this.globalDirectiveCallback = callback;

        // Apply to existing terminals
        for (Map.Entry<String, Terminal> entry : terminals.entrySet()) {
            String terminalId = entry.getKey();
            entry.getValue().transport.setDirectiveCallback(directive ->
                callback.onDirectiveReceived(terminalId, directive));
        }
    }

    /**
     * Set global error callback (applies to all terminals)
     */
    public void setGlobalErrorCallback(GlobalErrorCallback callback) {
        this.globalErrorCallback = callback;

        // Apply to existing terminals
        for (Map.Entry<String, Terminal> entry : terminals.entrySet()) {
            String terminalId = entry.getKey();
            entry.getValue().transport.setErrorCallback((errorCode, message) ->
                callback.onError(terminalId, errorCode, message));
        }
    }

    /**
     * Set global close callback (applies to all terminals)
     */
    public void setGlobalCloseCallback(GlobalCloseCallback callback) {
        this.globalCloseCallback = callback;

        // Apply to existing terminals
        for (Map.Entry<String, Terminal> entry : terminals.entrySet()) {
            String terminalId = entry.getKey();
            entry.getValue().transport.setCloseCallback((code, reason) ->
                callback.onConnectionClosed(terminalId, code, reason));
        }
    }

    /**
     * Get statistics for a terminal
     */
    public Statistics getTerminalStatistics(String terminalId) {
        Terminal terminal = terminals.get(terminalId);
        return terminal != null ? terminal.transport.getStatistics() : null;
    }

    /**
     * Get statistics for all terminals
     */
    public Map<String, Statistics> getAllStatistics() {
        Map<String, Statistics> stats = new HashMap<>();
        for (Map.Entry<String, Terminal> entry : terminals.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().transport.getStatistics());
        }
        return stats;
    }

    /**
     * Get last error message
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Clear error message
     */
    public void clearError() {
        lastError = "";
    }

    /**
     * Shutdown SDK (cleanup all resources)
     */
    public void shutdown() {
        logger.info("Shutting down SDK...");
        for (Terminal terminal : terminals.values()) {
            terminal.transport.shutdown();
        }
        terminals.clear();
        logger.info("SDK shutdown complete");
    }
}
