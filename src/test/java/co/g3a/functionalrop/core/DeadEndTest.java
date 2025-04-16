package co.g3a.functionalrop.core;

import co.g3a.functionalrop.ejemplo.AppError;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class DeadEndTest {

    private final DeadEnd deadEnd;

    DeadEndTest(){
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();//O usa Runnable::run para ejecución sincrónica.
        this.deadEnd = new DeadEnd(executor);
    }

    @Test
    void runSafe_success() {
        String input = "TestInput";

        CompletionStage<Result<String, AppError>> future = deadEnd.runSafeResultTransform(
                input,
                function -> {
                    System.out.println("✔️ Ejecutando efecto con: " + input);
                    return Result.success(input);
                },
                ex -> new AppError.DbError("Error inesperado: " + ex.getMessage())
        );

        Result<String, AppError> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("TestInput", result.getValue());
    }

    @Test
    void runSafe_failureWithMappedError() {
        String input = "Fallando";

        CompletionStage<Result<String, AppError>> future = deadEnd.runSafeResultTransform(
                input,
                val -> { throw new RuntimeException("💥 BOOM"); },
                ex -> new AppError.DbError("Falló con: " + ex.getMessage())
        );

        CompletionException ex = assertThrows(CompletionException.class, () -> future.toCompletableFuture().join());
        assertTrue(ex.getCause().getMessage().contains("💥 BOOM"));
    }

    @Test
    void runSafeResultTransform_success() {
        Integer input = 5;

        CompletionStage<Result<String, AppError>> future = deadEnd.runSafeResultTransform(
                input,
                val -> Result.success("Resultado calculado: " + (val * 2)),
                ex -> new AppError.ActivationCodeError("Transformación fallida: " + ex.getMessage())
        );

        Result<String, AppError> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertEquals("Resultado calculado: 10", result.getValue());
    }

}