package co.g3a.functionalrop;

import co.g3a.functionalrop.ejemplo.AppError;
import co.g3a.functionalrop.logging.StructuredLogger;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

public class DeadEndTest {

    StructuredLogger logger = (eventName, data) -> {
        System.out.println("🔔 Log: " + eventName + " -> " + data);
    };

    @Test
    void runSafe_success() {
        String input = "TestInput";

        CompletionStage<Result<String, AppError>> future = DeadEnd.runSafe(
                input,
                val -> System.out.println("✔️ Ejecutando efecto con: " + val),
                ex -> new AppError.DbError("Error inesperado: " + ex.getMessage()),
                "test_success_event",
                logger
        );

        Result<String, AppError> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("TestInput", result.getValue());
    }

    @Test
    void runSafe_failureWithMappedError() {
        String input = "Fallando";

        CompletionStage<Result<String, AppError>> future = DeadEnd.runSafe(
                input,
                val -> {
                    throw new RuntimeException("💥 BOOM");
                },
                ex -> new AppError.DbError("Falló con: " + ex.getMessage()),
                "test_failure_event",
                logger
        );

        Result<String, AppError> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertInstanceOf(AppError.DbError.class, result.getError());
        assertTrue(result.getError().toString().contains("💥 BOOM"));
    }

    @Test
    void runSafeTransform_success() {
        Integer input = 5;

        CompletionStage<Result<String, AppError>> future = DeadEnd.runSafeTransform(
                input,
                val -> "Resultado calculado: " + (val * 2),
                ex -> new AppError.ActivationCodeError("Transformación fallida: " + ex.getMessage()),
                "test_transform_success_event",
                logger
        );

        Result<String, AppError> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("Resultado calculado: 10", result.getValue());
    }

    @Test
    void runSafeTransform_failureWithMappedError() {
        Integer input = 42;

        CompletionStage<Result<String, AppError>> future = DeadEnd.runSafeTransform(
                input,
                val -> {
                    throw new RuntimeException("🔥 Error en transform");
                },
                ex -> new AppError.ActivationCodeError("Transformación fallida: " + ex.getMessage()),
                "test_transform_failure_event",
                logger
        );

        Result<String, AppError> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertInstanceOf(AppError.ActivationCodeError.class, result.getError());
        assertTrue(result.getError().toString().contains("🔥 Error en transform"));
    }
}