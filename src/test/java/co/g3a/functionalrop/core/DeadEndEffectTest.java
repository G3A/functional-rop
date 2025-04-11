package co.g3a.functionalrop.core;

import co.g3a.functionalrop.logging.StructuredLogger;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DeadEndEffectTest {

    @Test
    void runSafe_shouldExecuteEffectAndReturnSuccess() throws Exception {
        // 🧪 Setup
        Executor executor = Runnable::run; // synchronous executor for test simplicity
        DeadEnd deadEnd = new DeadEnd(executor);

        AtomicBoolean effectExecuted = new AtomicBoolean(false);

        Consumer<String> effect = input -> effectExecuted.set(true);

        Function<Throwable, String> errorMapper = Throwable::getMessage;

        StructuredLogger logger = new StructuredLogger() {
            @Override
            public void log(String eventName, Map<String, Object> context) {
                System.out.println("LOG [" + eventName + "]: " + context);
            }
        };

        // 🚀 Act
        CompletionStage<Result<String, String>> resultStage = deadEnd.runSafe(
                "hello",
                effect,
                errorMapper,
                "test-event",
                logger
        );

        Result<String, String> result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        // ✅ Assert
        assertTrue(effectExecuted.get(), "El efecto debe haberse ejecutado");
        assertTrue(result.isSuccess(), "El resultado debe ser éxito");
        assertEquals("hello", result.getValue());
    }

    @Test
    void runSafe_shouldReturnFailureOnException() throws Exception {
        Executor executor = Runnable::run;
        DeadEnd deadEnd = new DeadEnd(executor);

        Consumer<String> effect = input -> {
            throw new RuntimeException("Boom!");
        };

        Function<Throwable, String> errorMapper = Throwable::getMessage;

        StructuredLogger logger = new StructuredLogger() {
            @Override
            public void log(String eventName, Map<String, Object> context) {
                System.out.println("LOG [" + eventName + "]: " + context);
            }
        };

        CompletionStage<Result<String, String>> resultStage = deadEnd.runSafe(
                "test",
                effect,
                errorMapper,
                "error-event",
                logger
        );

        Result<String, String> result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        assertFalse(result.isSuccess(), "El resultado debe ser un fracaso");
        assertEquals("Boom!", result.getError());
    }
}