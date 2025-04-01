package co.g3a.functionalrop;

import co.g3a.functionalrop.logging.StructuredLogger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Utility class for safely executing side-effecting operations (dead-end functions),
 * both with and without transformation.
 * <p>
 * Supports structured logging and consistent functional error handling (Result.failure).
 */
public class DeadEnd {

    // ------------------------------------------------------------------------------------------------
    // 1️⃣ Dead-end function: performs effect, returns same value (for use in pipelines)
    // ------------------------------------------------------------------------------------------------

    public static <T> CompletionStage<Result<T>> runSafe(
            T input,
            Consumer<T> effect,
            String errorMessage,
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

                return Result.failure(errorMessage + ": " + e.getMessage());
            }
        });
    }

    /**
     * Overload of runSafe() without logger (no-op logger / silent).
     */
    public static <T> CompletionStage<Result<T>> runSafe(
            T input,
            Consumer<T> effect,
            String errorMessage
    ) {
        return runSafe(input, effect, errorMessage, "unknown_event", (e, d) -> {});
    }

    // ------------------------------------------------------------------------------------------------
    // 2️⃣ Transformer function: applies a function In -> Out with side effects, wraps in Result
    // ------------------------------------------------------------------------------------------------

    public static <In, Out> CompletionStage<Result<Out>> runSafeTransform(
            In input,
            DeadEndFunction<In, Out> function,
            String errorMessage,
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

                return Result.failure(errorMessage + ": " + e.getMessage());
            }
        });
    }

    /**
     * Overload of runSafeTransform() without logger.
     */
    public static <In, Out> CompletionStage<Result<Out>> runSafeTransform(
            In input,
            DeadEndFunction<In, Out> function,
            String errorMessage
    ) {
        return runSafeTransform(input, function, errorMessage, "unknown_event", (e, d) -> {});
    }
}