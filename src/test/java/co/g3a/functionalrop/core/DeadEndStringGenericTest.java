package co.g3a.functionalrop.core;

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

    private final DeadEnd deadEnd;

    DeadEndStringGenericTest(){
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();//O usa Runnable::run para ejecución sincrónica.
        this.deadEnd = new DeadEnd(executor);

    }

    @Test
    @DisplayName("🟢 runSafe - éxito con Result<String, String>")
    void runSafe_success_withStringError() {
        String input = "input correcto";

        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                input,
                function -> {
                    System.out.println("🙂 Procesando: " + input);
                    return Result.success(input);
                },
                ex -> "❌ Error capturado: " + ex.getMessage()
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals(input, result.getValue());
    }

    @Test
    @DisplayName("🔴 runSafe - falla con error mapeado (String)")
    void runSafe_failure_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                "valor de entrada",
                function -> { throw new RuntimeException("🔨 Error interno"); },
                ex -> "❌ Error simple: " + ex.getMessage()
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            future.toCompletableFuture().join();
        });

        String expectedMessage = "❌ Error simple: 🔨 Error interno";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    @DisplayName("🟢 runSafeTransform - éxito con String -> String")
    void runSafeResultTransform_success_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                100,
                value -> Result.success("Resultado: " + (value + 1)),
                ex -> "❌ Error transformando: " + ex.getMessage()
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("Resultado: 101", result.getValue());
    }

    @Test
    @DisplayName("🔴 runSafeTransform - con error de negocio")
    void runSafeResultTransform_failure_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                "entrada",
                val -> {
                    return Result.failure("⚠️ No se puede procesar");
                },
                ex -> "❌ Error transformado: " + ex.getMessage()
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().startsWith("⚠️ No se puede procesar"));

    }

    @Test
    @DisplayName("🔴 runSafeTransform - con excepción transformada a String")
    void runSafeResultTransform_exception_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                "entrada",
                val -> {
                    throw new RuntimeException("⚠️ No se puede procesar");
                },
                ex -> "❌ Error transformado: " + ex.getMessage()
        );
        Exception exception = assertThrows(RuntimeException.class, () -> {
            future.toCompletableFuture().join();
        });

        String expectedMessage = "❌ Error transformado: ⚠️ No se puede procesar";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));



    }
}