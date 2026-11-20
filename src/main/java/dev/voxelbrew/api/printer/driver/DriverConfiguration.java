package dev.voxelbrew.api.printer.driver;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;

public final class DriverConfiguration {
    private final Map<String, Object> properties;

    private static Object copyValue(Object value) {
        return value instanceof char[] password ? password.clone() : value;
    }

    public DriverConfiguration(@NonNull Map<String, Object> properties) {
        this.properties = properties.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key == null || key.isBlank()) {
                        throw new IllegalArgumentException("Configuration keys must be nonblank");
                    }
                    if (!(value instanceof String
                            || value instanceof Integer
                            || value instanceof Boolean
                            || value instanceof BigDecimal
                            || value instanceof URI
                            || value instanceof char[])) {
                        throw new IllegalArgumentException("Unsupported configuration value for key: " + key);
                    }
                    return Map.entry(key, copyValue(value));
                })
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Returns a detached snapshot for persistence. Contains secrets; never log it. */
    public Map<String, Object> properties() {
        return properties.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> copyValue(entry.getValue())));
    }

    /**
     * Converts settings to input values keyed by property name. Password arrays become their text
     * contents and decimals use plain notation. Contains secrets; never log the returned map.
     * @return an immutable input snapshot
     */
    public Map<String, String> toInput() {
        return properties.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> switch (entry.getValue()) {
                    case char[] password -> new String(password);
                    case BigDecimal decimal -> decimal.toPlainString();
                    default -> entry.getValue().toString();
                }));
    }

    @Override
    public String toString() {
        return "DriverConfiguration[redacted]";
    }

    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Property " + key + " is not of type " + type.getName());
        }
        return Optional.of(type.cast(copyValue(value)));
    }

    public Optional<Integer> getIntProperty(String key) {
        return getProperty(key, Integer.class);
    }

    public int requireIntProperty(String key) {
        return getIntProperty(key).orElseThrow(() -> new IllegalArgumentException("Missing required property: " + key));
    }

    public Optional<String> getStringProperty(String key) {
        return getProperty(key, String.class);
    }

    public String requireStringProperty(String key) {
        return getStringProperty(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required property: " + key));
    }

    public Optional<URI> getUriProperty(String key) {
        return getProperty(key, URI.class);
    }

    public URI requireUriProperty(String key) {
        return getUriProperty(key).orElseThrow(() -> new IllegalArgumentException("Missing required property: " + key));
    }

    public Optional<char[]> getPasswordProperty(String key) {
        return getProperty(key, char[].class);
    }

    public char[] requirePasswordProperty(String key) {
        return getPasswordProperty(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required property: " + key));
    }
}
