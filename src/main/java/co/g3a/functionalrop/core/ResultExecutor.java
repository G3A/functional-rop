package co.g3a.functionalrop.core;

import co.g3a.functionalrop.logging.StructuredLogger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Ejecuta tareas asíncronas con medición de tiempo y logging estructurado.
 */
public class ResultExecutor {

    private final Executor executor;

    public ResultExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Ejecuta una tarea asíncrona, registrando tiempo y resultado.
     *
     * @param task      tarea que retorna un Result
     * @param eventName nombre del evento para logging
     * @param logger    instancia de logger estructurado
     * @param <T>       tipo del valor exitoso
     * @param> <E>       tipo del error
     * @return etapa asíncrona con el resultado
     */
    public <T, E> CompletionStage<Result<T, E>> runAsync(
            Supplier<Result<T, E>> task,
            String eventName,
            StructuredLogger logger
    ) {
        long start = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Result<T, E> result = task.get();
                long duration = System.currentTimeMillis() - start;
                logResult(logger, eventName, result, duration, null);
                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                logResult(logger, eventName, Result.failure(null), duration, e.getMessage());
                throw e;
            }
        }, executor).exceptionally(ex -> {
            long duration = System.currentTimeMillis() - start;
            logResult(logger, eventName, Result.failure(null), duration, ex.getMessage());
            return Result.failure(null);
        });
    }

    /**
     * Registra el evento estructurado según el resultado.
     *
     * @param logger    instancia del logger
     * @param eventName nombre del evento
     * @param result    resultado exitoso o fallido
     * @param duration  tiempo transcurrido (ms)
     * @param error     mensaje de error (opcional)
     */
    private static <T, E> void logResult(StructuredLogger logger, String eventName, Result<T, E> result, long duration, String error) {
        Map<String, Object> context = Map.of(
                "duration_ms", duration,
                "status", result.isSuccess() ? "success" : "failure",
                "error", error != null ? error : "",
                "value", result.isSuccess() ? result.getValue() : "N/A"
        );
        logger.log(eventName, context);
    }
}