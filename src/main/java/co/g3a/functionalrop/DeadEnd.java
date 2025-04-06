package co.g3a.functionalrop;

import co.g3a.functionalrop.logging.StructuredLogger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class DeadEnd {

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