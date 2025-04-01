package co.g3a.functionalrop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Result<T> representa una operación que puede devolver un valor (Success)
 * o un error (Failure), tipo Option + Either simplificado.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();

    T getValue();
    String getError();

    // 🟢 Factories
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    // 🧠 map: transforma el valor si fue exitoso
    default <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success<T> s -> success(mapper.apply(s.value()));
            case Failure<T> f -> failure(f.error());
        };
    }

    // ➿ flatMap: encadena resultados
    default <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        return switch (this) {
            case Success<T> s -> mapper.apply(s.value());
            case Failure<T> f -> failure(f.error());
        };
    }

    // ⚙️ flatMap async
    default <U> CompletionStage<Result<U>> flatMapAsync(Function<? super T, CompletionStage<Result<U>>> asyncMapper) {
        return switch (this) {
            case Success<T> s -> asyncMapper.apply(s.value());
            case Failure<T> f -> CompletableFuture.completedFuture(failure(f.error()));
        };
    }

    // ✅ Success
    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public T getValue() { return value; }
        @Override public String getError() {
            throw new IllegalStateException("No error in success");
        }
    }

    // ❌ Failure
    record Failure<T>(String error) implements Result<T> {
        @Override public boolean isSuccess() { return false; }
        @Override public T getValue() {
            throw new IllegalStateException("No value in failure");
        }
        @Override public String getError() { return error; }
    }

}

