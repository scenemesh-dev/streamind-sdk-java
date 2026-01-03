package com.streamind.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Signal - Uplink message from terminal to platform
 */
public class Signal {
    private static final AtomicLong counter = new AtomicLong(0);
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private String uuid;
    private String type;
    private String timestamp;
    private SignalSource source;
    private Payload payload;

    public Signal(String type) {
        this.uuid = generateUuid();
        this.type = type;
        this.timestamp = ISO_FORMATTER.format(Instant.now());
        this.source = new SignalSource();
        this.source.setGeneratedTime(this.timestamp);
        this.payload = new Payload();
    }

    private static String generateUuid() {
        long now = System.currentTimeMillis();
        long count = counter.incrementAndGet();
        return "sig_" + now + "_" + count;
    }

    // Getters and setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public SignalSource getSource() { return source; }
    public void setSource(SignalSource source) { this.source = source; }

    public Payload getPayload() { return payload; }
    public void setPayload(Payload payload) { this.payload = payload; }

    /**
     * Convert to JSON string
     */
    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid);
        json.addProperty("type", type);
        json.addProperty("timestamp", timestamp);
        json.add("source", gson.toJsonTree(source));
        json.add("payload", gson.toJsonTree(payload.getData()));
        return gson.toJson(json);
    }

    /**
     * Create from JSON string
     */
    public static Signal fromJson(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        String type = json.has("type") ? json.get("type").getAsString() : "";
        Signal signal = new Signal(type);

        if (json.has("uuid")) {
            signal.uuid = json.get("uuid").getAsString();
        }
        if (json.has("timestamp")) {
            signal.timestamp = json.get("timestamp").getAsString();
        }
        if (json.has("source")) {
            signal.source = gson.fromJson(json.get("source"), SignalSource.class);
        }
        if (json.has("payload")) {
            signal.payload = Payload.fromJson(json.get("payload").toString());
        }

        return signal;
    }

    /**
     * Signal Source Information
     */
    public static class SignalSource {
        private String receptorId = "";
        private String receptorTopic = "";
        private String generatedTime = "";

        public String getReceptorId() { return receptorId; }
        public void setReceptorId(String receptorId) { this.receptorId = receptorId; }

        public String getReceptorTopic() { return receptorTopic; }
        public void setReceptorTopic(String receptorTopic) { this.receptorTopic = receptorTopic; }

        public String getGeneratedTime() { return generatedTime; }
        public void setGeneratedTime(String generatedTime) { this.generatedTime = generatedTime; }
    }
}
