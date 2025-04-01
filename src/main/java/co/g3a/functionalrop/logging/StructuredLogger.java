package co.g3a.functionalrop.logging;

import java.util.Map;

@FunctionalInterface
public interface StructuredLogger {
    void log(String event, Map<String, Object> data);
}