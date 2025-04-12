package co.g3a.functionalrop.core;

import co.g3a.functionalrop.logging.StructuredLogger;
import co.g3a.functionalrop.utils.DeadEndFunction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Clase de utilidad para ejecutar de forma segura operaciones con efectos secundarios (funciones sin salida o dead-end functions),
 * con y sin transformación.
 * <p>
 * Admite registro estructurado y gestión consistente de errores funcionales (Result.failure).
 */
public class DeadEnd {

    private final Executor executor;

    public DeadEnd(Executor executor) {
        this.executor = executor;
    }

    // ------------------------------------------------------------------------------------------------
    // 1️⃣ Dead-end function: performs effect, returns same value (for use in pipelines)
    // ------------------------------------------------------------------------------------------------

    public <T, E> CompletionStage<Result<T, E>> runSafe(
            T input,
            Consumer<T> effect,
            Function<Throwable, E> errorMapper,
            String eventName,
            StructuredLogger logger
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                effect.accept(input);
                long duration = System.currentTimeMillis() - start;
                logger.log(eventName, Map.of(
                        "status", "success",
                        "duration_ms", duration,
                        "input", input
                ));
                return Result.success(input);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                logger.log(eventName, Map.of(
                        "status", "failure",
                        "duration_ms", duration,
                        "error", e.getMessage(),
                        "input", input
                ));
                return Result.failure(errorMapper.apply(e));
            }
        }, executor);
    }

    // ------------------------------------------------------------------------------------------------
    // 2️⃣ Transformer function: applies a function In -> Out with side effects, wraps in Result
    // ------------------------------------------------------------------------------------------------
    public <In, Out, E> CompletionStage<Result<Out, E>> runSafeResultTransform(
            In input,
            Function<In, Result<Out, E>> function,
            Function<Throwable, E> errorMapper,
            String eventName,
            StructuredLogger logger) {

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                Result<Out, E> result = function.apply(input);

                long duration = System.currentTimeMillis() - startTime;

                if (result.isSuccess()) {
                    logger.log(eventName, Map.of(
                            "status", "success",
                            "duration_ms", duration,
                            "input", input,
                            "output", result.getValue()
                    ));
                } else {
                    logger.log(eventName, Map.of(
                            "status", "failure",
                            "duration_ms", duration,
                            "input", input,
                            "error", result.getError().toString()
                    ));
                }

                return result;

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;

                logger.log(eventName, Map.of(
                        "status", "exception",
                        "duration_ms", duration,
                        "error", e.getMessage(),
                        "input", input
                ));

                return Result.failure(errorMapper.apply(e));
            }
        }, this.executor);
    }
}