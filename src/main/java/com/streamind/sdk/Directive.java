package com.streamind.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Directive - Downlink command from platform to terminal
 */
public class Directive {
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private String id;
    private String name;
    private String timestamp;
    private Map<String, Object> parameters;

    public Directive() {
        this("", "", new HashMap<>());
    }

    public Directive(String id, String name, Map<String, Object> parameters) {
        this.id = id;
        this.name = name;
        this.timestamp = ISO_FORMATTER.format(Instant.now());
        this.parameters = new HashMap<>(parameters);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(parameters);
    }

    /**
     * Get string parameter
     */
    public String getStringParameter(String key, String defaultValue) {
        Object value = parameters.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    /**
     * Get integer parameter
     */
    public int getIntParameter(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get number parameter as double
     */
    public double getNumberParameter(String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get boolean parameter
     */
    public boolean getBoolParameter(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if ("true".equalsIgnoreCase(String.valueOf(value))) {
            return true;
        }
        if ("false".equalsIgnoreCase(String.valueOf(value))) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Get object parameter
     */
    public Object getObjectParameter(String key, Object defaultValue) {
        Object value = parameters.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Convert to JSON string
     */
    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("timestamp", timestamp);
        json.add("parameters", gson.toJsonTree(parameters));
        return gson.toJson(json);
    }

    /**
     * Create from JSON string
     *
     * Handles both object and string formats for parameters field
     */
    @SuppressWarnings("unchecked")
    public static Directive fromJson(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        String id = json.has("id") ? json.get("id").getAsString() : "";
        String name = json.has("name") ? json.get("name").getAsString() : "";

        Map<String, Object> params = new HashMap<>();

        if (json.has("parameters")) {
            JsonElement paramsElement = json.get("parameters");

            if (paramsElement.isJsonObject()) {
                // Parameters is an object
                params = gson.fromJson(paramsElement, Map.class);
            } else if (paramsElement.isJsonPrimitive() && paramsElement.getAsJsonPrimitive().isString()) {
                // Parameters is a string, need to parse it
                String paramsStr = paramsElement.getAsString();
                if (!paramsStr.isEmpty()) {
                    try {
                        JsonElement parsed = JsonParser.parseString(paramsStr);
                        if (parsed.isJsonObject()) {
                            params = gson.fromJson(parsed, Map.class);
                        }
                    } catch (Exception e) {
                        // Invalid JSON string, keep empty
                    }
                }
            }
        }

        return new Directive(id, name, params);
    }
}
