package co.g3a.functionalrop;

import co.g3a.functionalrop.logging.StructuredLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Slf4jStructuredLogger implements StructuredLogger {
    private static final Logger log = LoggerFactory.getLogger("Structured");

    @Override
    public void log(String event, Map<String, Object> data) {
        log.info("event={} {}", event, data); // Log en formato key=value
    }
}