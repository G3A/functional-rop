package co.g3a.functionalrop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Result<T, E> representa una operación que puede devolver un valor (Success)
 * o un error (Failure), tipo Option + Either simplificado.
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    boolean isSuccess();
    T getValue();
    E getError();

    // 🟢 Factories
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    // 🧠 map: transforma el valor si fue exitoso
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return isSuccess() ? success(mapper.apply(getValue())) : failure(getError());
    }

    // ➿ flatMap: encadena resultados
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return isSuccess() ? mapper.apply(getValue()) : failure(getError());
    }

    // ⚙️ flatMap async
    default <U> CompletionStage<Result<U, E>> flatMapAsync(Function<T, CompletionStage<Result<U, E>>> mapper) {
        return isSuccess() ? mapper.apply(getValue()) : CompletableFuture.completedFuture(failure(getError()));
    }

    // ✅ Success
    record Success<T, E>(T value) implements Result<T, E> {
        public boolean isSuccess() { return true; }
        public T getValue() { return value; }
        public E getError() { throw new UnsupportedOperationException("Success has no error"); }
    }

    // ❌ Failure
    record Failure<T, E>(E error) implements Result<T, E> {
        public boolean isSuccess() { return false; }
        public T getValue() { throw new UnsupportedOperationException("Failure has no value"); }
        public E getError() { return error; }
    }


    // Obtiene el resultado final, ya sea Success o Failure
    default <U> U fold(Function<E, U> onError, Function<T, U> onSuccess) {
        return isSuccess() ? onSuccess.apply(getValue()) : onError.apply(getError());
    }
}