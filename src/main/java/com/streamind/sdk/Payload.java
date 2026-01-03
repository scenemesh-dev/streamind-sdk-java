package com.streamind.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Payload - Key-value data container
 */
public class Payload {
    private final Map<String, Object> data;
    private static final Gson gson = new Gson();

    public Payload() {
        this.data = new HashMap<>();
    }

    /**
     * Set string value
     */
    public void setString(String key, String value) {
        data.put(key, value);
    }

    /**
     * Get string value
     */
    public String getString(String key, String defaultValue) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    /**
     * Set number value
     */
    public void setNumber(String key, Number value) {
        data.put(key, value);
    }

    /**
     * Get number value as double
     */
    public double getNumber(String key, double defaultValue) {
        Object value = data.get(key);
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
     * Get integer value
     */
    public int getInt(String key, int defaultValue) {
        Object value = data.get(key);
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
     * Set boolean value
     */
    public void setBool(String key, boolean value) {
        data.put(key, value);
    }

    /**
     * Get boolean value
     */
    public boolean getBool(String key, boolean defaultValue) {
        Object value = data.get(key);
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
     * Set object value
     */
    public void setObject(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Get object value
     */
    public Object getObject(String key, Object defaultValue) {
        Object value = data.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get all data
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    /**
     * Set all data
     */
    public void setData(Map<String, Object> data) {
        this.data.clear();
        this.data.putAll(data);
    }

    /**
     * Clear all data
     */
    public void clear() {
        data.clear();
    }

    /**
     * Convert to JSON string
     */
    public String toJson() {
        return gson.toJson(data);
    }

    /**
     * Create from JSON string
     */
    public static Payload fromJson(String jsonStr) {
        Payload payload = new Payload();
        try {
            JsonElement element = JsonParser.parseString(jsonStr);
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                Map<String, Object> map = gson.fromJson(jsonObject, Map.class);
                payload.setData(map);
            }
        } catch (Exception e) {
            // Invalid JSON, keep empty
        }
        return payload;
    }
}
