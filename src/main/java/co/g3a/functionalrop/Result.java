package co.g3a.functionalrop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    boolean isSuccess();
    T getValue();
    E getError();

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    default <U> Result<U, E> map(Function<T, U> mapper) {
        return isSuccess() ? success(mapper.apply(getValue())) : failure(getError());
    }

    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return isSuccess() ? mapper.apply(getValue()) : failure(getError());
    }

    default <U> CompletionStage<Result<U, E>> flatMapAsync(Function<T, CompletionStage<Result<U, E>>> mapper) {
        return isSuccess() ? mapper.apply(getValue()) : CompletableFuture.completedFuture(failure(getError()));
    }

    record Success<T, E>(T value) implements Result<T, E> {
        public boolean isSuccess() { return true; }
        public T getValue() { return value; }
        public E getError() { throw new UnsupportedOperationException("Success has no error"); }
    }

    record Failure<T, E>(E error) implements Result<T, E> {
        public boolean isSuccess() { return false; }
        public T getValue() { throw new UnsupportedOperationException("Failure has no value"); }
        public E getError() { return error; }
    }
}