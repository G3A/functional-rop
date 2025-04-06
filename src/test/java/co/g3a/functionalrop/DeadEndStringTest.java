package co.g3a.functionalrop;

import co.g3a.functionalrop.logging.StructuredLogger;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

public class DeadEndStringTest {

    StructuredLogger logger = (event, data) -> System.out.println("📋 LOG [" + event + "]: " + data);

    @Test
    void runSafe_success_withStringError() {
        String input = "OK";

        CompletionStage<Result<String, String>> future = DeadEnd.runSafe(
                input,
                val -> System.out.println("Procesando: " + val),
                ex -> "⚠️ Error capturado: " + ex.getMessage(),
                "runSafe_success",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("OK", result.getValue());
    }

    @Test
    void runSafe_failure_withStringError() {
        String input = "FAIL";

        CompletionStage<Result<String, String>> future = DeadEnd.runSafe(
                input,
                val -> {
                    throw new RuntimeException("💥 Excepción controlada");
                },
                ex -> "⚠️ Error capturado: " + ex.getMessage(),
                "runSafe_failure",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("⚠️ Error capturado"));
        assertTrue(result.getError().contains("💥 Excepción controlada"));
    }

    @Test
    void runSafeTransform_success_withStringError() {
        CompletionStage<Result<String, String>> future = DeadEnd.runSafeTransform(
                10,
                value -> "Resultado calculado: " + (value + 5),
                ex -> "❌ Fallo al transformar: " + ex.getMessage(),
                "runSafeTransform_success",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("Resultado calculado: 15", result.getValue());
    }

    @Test
    void runSafeTransform_failure_withStringError() {
        CompletionStage<Result<String, String>> future = DeadEnd.runSafeTransform(
                123,
                value -> {
                    throw new IllegalArgumentException("¡Transformación no permitida!");
                },
                ex -> "❌ Fallo al transformar: " + ex.getMessage(),
                "runSafeTransform_failure",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().startsWith("❌ Fallo al transformar:"));
        assertTrue(result.getError().contains("Transformación no permitida"));
    }
}