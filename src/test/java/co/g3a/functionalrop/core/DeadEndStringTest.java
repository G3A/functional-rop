package co.g3a.functionalrop.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class DeadEndStringTest {



    private final DeadEnd deadEnd;

    DeadEndStringTest(){
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();//O usa Runnable::run para ejecución sincrónica.
        this.deadEnd = new DeadEnd(executor);
    }

    @Test
    void runSafe_success_withStringError() {
        String input = "OK";

        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                input,
                function -> {
                    System.out.println("Procesando: " + input);
                    return Result.success(input);
                    },
                ex -> "⚠️ Error capturado: " + ex.getMessage()
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("OK", result.getValue());
    }

    @Test
    void runSafe_failure_withStringError() {
        String input = "FAIL";

        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                input,
                val -> {
                    throw new RuntimeException("💥 Excepción controlada");
                },
                ex -> "⚠️ Error capturado: " + ex.getMessage()
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            future.toCompletableFuture().join();
        });

        String expectedMessage = "⚠️ Error capturado: 💥 Excepción controlada";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void runSafeResultTransform_success_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                10,
                value -> Result.success("Resultado calculado: " + (value + 5)),
                ex -> "❌ Fallo al transformar: " + ex.getMessage()
        );

        Result<String, String> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("Resultado calculado: 15", result.getValue());
    }

    @Test
    void runSafeResultTransform_failure_withStringError() {
        CompletionStage<Result<String, String>> future = deadEnd.runSafeResultTransform(
                123,
                value -> {
                    throw new IllegalArgumentException("¡Transformación no permitida!");
                },
                ex -> "❌ Fallo al transformar: " + ex.getMessage()
        );
        Exception exception = assertThrows(RuntimeException.class, () -> {
            future.toCompletableFuture().join();
        });

        String expectedMessage = "❌ Fallo al transformar: ¡Transformación no permitida!";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}