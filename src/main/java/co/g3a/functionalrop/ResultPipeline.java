package co.g3a.functionalrop;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Fluent wrapper para Result<T, E> + CompletionStage.
 */
public class ResultPipeline<T, E> {

    private final CompletionStage<Result<T, E>> result;

    private ResultPipeline(CompletionStage<Result<T, E>> result) {
        this.result = result;
    }

    public static <T, E> ResultPipeline<T, E> use(T value) {
        return new ResultPipeline<>(CompletableFuture.completedFuture(Result.success(value)));
    }

    public ResultPipeline<T, E> validate(Function<T, ValidationResult<T>> validator, Function<String, E> errorMapper) {
        CompletionStage<Result<T, E>> newResult = result.thenApply(res -> {
            if (!res.isSuccess()) return res;
            ValidationResult<T> validation = validator.apply(res.getValue());
            if (!validation.isValid()) {
                return Result.failure(errorMapper.apply(validation.getErrors().get(0)));
            }
            return Result.success(validation.getValue());
        });
        return new ResultPipeline<>(newResult);
    }

    public <U> ResultPipeline<U, E> map(Function<T, U> mapper) {
        CompletionStage<Result<U, E>> newResult = result.thenApply(res -> res.map(mapper));
        return new ResultPipeline<>(newResult);
    }

    public <U> ResultPipeline<U, E> flatMapAsync(Function<T, CompletionStage<Result<U, E>>> mapper) {
        CompletionStage<Result<U, E>> newResult = result.thenCompose(res ->
                res.isSuccess()
                        ? mapper.apply(res.getValue())
                        : CompletableFuture.completedFuture(Result.failure(res.getError()))
        );
        return new ResultPipeline<>(newResult);
    }

    public CompletionStage<Result<T, E>> build() {
        return result;
    }

    public CompletionStage<Void> thenAccept(java.util.function.Consumer<Result<T, E>> consumer) {
        return result.thenAccept(consumer);
    }

    // 🔥 Nuevo método para ejecutar tareas en paralelo con manejo de múltiples errores
    public static <T, E> CompletionStage<Result<List<Object>, E>> runInParallel(
            T input,
            List<Function<T, CompletionStage<Result<?, E>>>> tasks,
            Function<List<E>, E> errorCombiner
    ) {
        List<CompletionStage<Result<?, E>>> futures = tasks.stream()
                .map(task -> task.apply(input))
                .toList();

        List<CompletableFuture<Result<?, E>>> cfList = futures.stream()
                .map(CompletionStage::toCompletableFuture)
                .toList();

        return CompletableFuture.allOf(cfList.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<Object> results = new ArrayList<>();
                    List<E> errores = new ArrayList<>();

                    for (CompletableFuture<Result<?, E>> cf : cfList) {
                        Result<?, E> result = cf.join();
                        if (result.isSuccess()) {
                            results.add(result.getValue());
                        } else {
                            errores.add(result.getError());
                        }
                    }

                    if (!errores.isEmpty()) {
                        return Result.failure(errorCombiner.apply(errores));
                    }

                    return Result.success(results);
                });
    }

    public static <Input, Output, E> CompletionStage<Result<List<Output>, E>> runInParallelTyped(
            Input input,
            List<Function<Input, CompletionStage<Result<Output, E>>>> tasks,
            Function<List<E>, E> errorCombiner
    ) {
        List<CompletableFuture<Result<Output, E>>> futures = tasks.stream()
                .map(task -> task.apply(input).toCompletableFuture())
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<Output> results = new ArrayList<>();
                    List<E> errors = new ArrayList<>();

                    for (CompletableFuture<Result<Output, E>> future : futures) {
                        Result<Output, E> res = future.join();
                        if (res.isSuccess()) {
                            results.add(res.getValue());
                        } else {
                            errors.add(res.getError());
                        }
                    }

                    if (!errors.isEmpty()) {
                        return Result.failure(errorCombiner.apply(errors));
                    }

                    return Result.success(results);
                });
    }
}