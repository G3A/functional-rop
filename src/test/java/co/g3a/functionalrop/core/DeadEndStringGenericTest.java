package co.g3a.functionalrop.core;

import co.g3a.functionalrop.logging.StructuredLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demuestra que DeadEnd puede funcionar sin ningún tipo de error de dominio como AppError
 * utilizando simplemente Result<String, String>.
 */
public class DeadEndStringGenericTest {

    // Logger de prueba (simula log estructurado)
    StructuredLogger logger = (event, data) -> {
        System.out.println("🔍 EVENT: " + event + " 👉 DATA: " + data);
    };
    private final DeadEnd deadEnd;

    DeadEndStringGenericTest(){
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();//O usa Runnable::run para ejecución sincrónica.
        this.deadEnd = new DeadEnd(executor);

    }

    @Test
    @DisplayName("🟢 runSafe - éxito con Result<String, String>")
    void runSafe_success_withStringError() {
        String input = "input correcto";

        CompletionStage<Result<String, String>> future = deadEnd.runSafe(
                input,
                value -> System.out.println("🙂 Procesando: " + value),
                ex -> "❌ Error capturado: " + ex.getMessage(),
                "runSafe_test_success",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals(input, result.getValue());
    }

    @Test
    @DisplayName("🔴 runSafe - falla con error mapeado (String)")
    void runSafe_failure_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafe(
                "valor de entrada",
                val -> { throw new RuntimeException("🔨 Error interno"); },
                ex -> "❌ Error simple: " + ex.getMessage(),
                "runSafe_test_failure",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().startsWith("❌ Error simple:"));
        assertTrue(result.getError().contains("🔨 Error interno"));
    }

    @Test
    @DisplayName("🟢 runSafeTransform - éxito con String -> String")
    void runSafeTransform_success_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeTransform(
                100,
                value -> "Resultado: " + (value + 1),
                ex -> "❌ Error transformando: " + ex.getMessage(),
                "runSafeTransform_success",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("Resultado: 101", result.getValue());
    }

    @Test
    @DisplayName("🔴 runSafeTransform - con excepción transformada a String")
    void runSafeTransform_failure_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeTransform(
                "entrada",
                val -> {
                    throw new IllegalStateException("⚠️ No se puede procesar");
                },
                ex -> "❌ Error transformado: " + ex.getMessage(),
                "runSafeTransform_failure",
                logger
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().startsWith("❌ Error transformado:"));
        assertTrue(result.getError().contains("⚠️ No se puede procesar"));
    }
}