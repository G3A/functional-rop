package co.g3a.functionalrop.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Representa una operación que puede resultar en éxito (valor) o fracaso (error).
 * <p>
 * Inspirado en Option + Either, simplificado para Java.
 *
 * @param <T> Tipo del valor exitoso
 * @param <E> Tipo del error
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /**
     * @return true si el resultado es exitoso
     */
    boolean isSuccess();

    /**
     * Obtiene el valor exitoso.
     *
     * @throws UnsupportedOperationException si es un fracaso
     */
    T getValue();

    /**
     * Obtiene el error.
     *
     * @throws UnsupportedOperationException si es un éxito
     */
    E getError();

    // 🟢 Factories

    /**
     * Crea un resultado exitoso.
     *
     * @param value valor de éxito
     * @param <T>   tipo del valor
     * @param <E>   tipo del error (no usado aquí)
     * @return instancia de Success
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Crea un resultado fallido.
     *
     * @param error error ocurrido
     * @param <T>   tipo del valor (no disponible)
     * @param <E>   tipo del error
     * @return instancia de Failure
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    // 🧠 map: transforma el valor si fue exitoso

    /**
     * Aplica una función al valor exitoso, si existe.
     *
     * @param mapper función T -> U
     * @param <U>    nuevo tipo de valor
     * @return nuevo Result (éxito o mismo fracaso)
     */
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return isSuccess() ? success(mapper.apply(getValue())) : failure(getError());
    }

    /**
     * Transforma el valor del error ({@code failure}) de este {@link Result} en otro tipo,
     * sin afectar el valor en caso de éxito.
     * <p>
     * Este método es útil cuando se desea propagar el error a otra capa del sistema
     * que maneja un tipo de error diferente, como por ejemplo, cuando se traduce un
     * error de validación de un Value Object a un error específico de un caso de uso.
     * <p>
     * Si este {@code Result} es un éxito, se retorna tal cual sin cambios.
     * Si es un error, se aplica el {@code mapper} para convertir el error al nuevo tipo.
     *
     * <pre>{@code
     * Result<Email, EmailError> emailResult = Email.create("test@example.com");
     * Result<Email, UseCaseError> mappedResult = emailResult.mapFailure(this::mapEmailToUseCaseError);
     * }</pre>
     *
     * @param mapper función que transforma el error original ({@code E}) al nuevo tipo de error ({@code U})
     * @param <U>    el nuevo tipo de error con el que se construirá el {@code Result} en caso de error
     * @return un nuevo {@code Result} con el mismo valor en caso de éxito, o con el error transformado en caso de fallo
     */
    default <U> Result<T, U> mapFailure(Function<E, U> mapper) {
        return this.isSuccess()
                ? Result.success(this.getValue())
                : Result.failure(mapper.apply(this.getError()));
    }

    // ➿ flatMap: encadena resultados

    /**
     * Encadena otra operación que retorna un Result.
     *
     * @param mapper función T -> Result&lt;U, E&gt;
     * @param <U>    tipo del nuevo valor
     * @return resultado anidado (éxito o propagación del fracaso)
     */
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return isSuccess() ? mapper.apply(getValue()) : failure(getError());
    }

    // ⚙️ flatMap async

    /**
     * Versión asíncrona de {@link #flatMap(Function)}.
     *
     * @param mapper función T -> CompletionStage&lt;Result&lt;U, E&gt;&gt;
     * @param <U>    tipo del nuevo valor
     * @return etapa asíncrona con el resultado encadenado
     */
    default <U> CompletionStage<Result<U, E>> flatMapAsync(Function<T, CompletionStage<Result<U, E>>> mapper) {
        return isSuccess() ? mapper.apply(getValue()) : CompletableFuture.completedFuture(failure(getError()));
    }

    /**
     * Combina ambos caminos (éxito/fracaso) en un solo valor final.
     *
     * @param onError    función para manejar el error
     * @param onSuccess  función para manejar el valor exitoso
     * @param <U>        tipo del resultado final
     * @return valor final U
     */
    default <U> U fold(Function<E, U> onError, Function<T, U> onSuccess) {
        return isSuccess() ? onSuccess.apply(getValue()) : onError.apply(getError());
    }

    /**
     * Resultado exitoso (contiene el valor).
     *
     * @param value valor de tipo T
     * @param <T>   tipo del valor
     * @param <E>   tipo del error (no usado)
     */
    record Success<T, E>(T value) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public E getError() {
            throw new UnsupportedOperationException("Success has no error");
        }
    }

    /**
     * Resultado fallido (contiene el error).
     *
     * @param error error de tipo E
     * @param <T>   tipo del valor (no disponible)
     * @param <E>   tipo del error
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T getValue() {
            throw new UnsupportedOperationException("Failure has no value");
        }

        @Override
        public E getError() {
            return error;
        }
    }
}