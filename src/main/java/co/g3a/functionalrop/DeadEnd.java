package co.g3a.functionalrop;

import co.g3a.functionalrop.logging.StructuredLogger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Clase de utilidad para ejecutar de forma segura operaciones con efectos secundarios (funciones sin salida o dead-end functions),
 * con y sin transformación.
 * <p>
 * Admite registro estructurado y gestión consistente de errores funcionales (Result.failure).
 */
public class DeadEnd {

    // ------------------------------------------------------------------------------------------------
    // 1️⃣ Dead-end function: performs effect, returns same value (for use in pipelines)
    // ------------------------------------------------------------------------------------------------

    public static <T, E> CompletionStage<Result<T, E>> runSafe(
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
        });
    }

    // ------------------------------------------------------------------------------------------------
    // 2️⃣ Transformer function: applies a function In -> Out with side effects, wraps in Result
    // ------------------------------------------------------------------------------------------------

    public static <In, Out, E> CompletionStage<Result<Out, E>> runSafeTransform(
            In input,
            DeadEndFunction<In, Out> function,
            Function<Throwable, E> errorMapper,
            String eventName,
            StructuredLogger logger
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                Out output = function.apply(input);
                long duration = System.currentTimeMillis() - start;
                logger.log(eventName, Map.of(
                        "status", "success",
                        "duration_ms", duration,
                        "input", input,
                        "output", output
                ));
                return Result.success(output);
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
        });
    }
}