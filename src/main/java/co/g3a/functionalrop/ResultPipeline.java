package co.g3a.functionalrop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Fluent wrapper para Result<T> + CompletionStage.
 */
public class ResultPipeline<T> {

    private final CompletionStage<Result<T>> stage;

    public ResultPipeline(CompletionStage<Result<T>> stage) {
        this.stage = stage;
    }

    public static <T> ResultPipeline<T> use(T input) {
        return new ResultPipeline<>(CompletableFuture.completedFuture(Result.success(input)));
    }

    public static <T> ResultPipeline<T> fromResult(Result<T> result) {
        return new ResultPipeline<>(CompletableFuture.completedFuture(result));
    }

    public static <T> ResultPipeline<T> fromAsync(CompletionStage<Result<T>> stage) {
        return new ResultPipeline<>(stage);
    }

    public <U> ResultPipeline<U> map(Function<? super T, ? extends U> mapper) {
        CompletionStage<Result<U>> newStage = stage.thenApply(result -> result.map(mapper));
        return new ResultPipeline<>(newStage);
    }

    public <U> ResultPipeline<U> flatMap(Function<? super T, Result<U>> mapper) {
        CompletionStage<Result<U>> newStage = stage.thenApply(result -> result.flatMap(mapper));
        return new ResultPipeline<>(newStage);
    }

    public <U> ResultPipeline<U> flatMapAsync(Function<? super T, CompletionStage<Result<U>>> asyncMapper) {
        CompletionStage<Result<U>> newStage = stage.thenCompose(result -> result.flatMapAsync(asyncMapper));
        return new ResultPipeline<>(newStage);
    }

    public ResultPipeline<T> validate(Function<T, ValidationResult<T>> validator) {
        CompletionStage<Result<T>> newStage = stage.thenApply(res -> {
            if (!res.isSuccess()) return res;
            ValidationResult<T> val = validator.apply(res.getValue());
            return val.isValid()
                ? Result.success(val.getValue())
                : Result.failure(String.join(", ", val.getErrors()));
        });
        return new ResultPipeline<>(newStage);
    }

    public CompletionStage<Result<T>> toResultAsync() {
        return stage;
    }

    public void thenAccept(java.util.function.Consumer<Result<T>> consumer) {
        stage.thenAccept(consumer);
    }
}