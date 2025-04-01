package co.g3a.functionalrop.logging;

import java.util.Map;

public class ConsoleStructuredLogger implements StructuredLogger {

    @Override
    public void log(String event, Map<String, Object> data) {
        System.out.println("[LOG] " + event + " → " + data);
    }
}