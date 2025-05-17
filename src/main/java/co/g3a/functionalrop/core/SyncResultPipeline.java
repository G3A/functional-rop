package co.g3a.functionalrop.core;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@code SyncResultPipeline} es una API fluida y completamente síncrona que permite encadenar operaciones
 * funcionales sobre un {@link Result}. Está diseñada para flujos donde se requiere mantener la lógica
 * declarativa, pero sin introducir asincronía ni pérdida del contexto transaccional.
 *
 * @param <T> tipo del valor exitoso
 * @param <E> tipo del error
 */
public class SyncResultPipeline<T, E> {

    private final Result<T, E> result;

    private SyncResultPipeline(Result<T, E> result) {
        this.result = result;
    }

    /**
     * Inicializa la pipeline con un valor exitoso.
     */
    public static <T, E> SyncResultPipeline<T, E> use(T value) {
        return new SyncResultPipeline<>(Result.success(value));
    }

    /**
     * Inicializa la pipeline con un resultado existente.
     */
    public static <T, E> SyncResultPipeline<T, E> from(Result<T, E> result) {
        return new SyncResultPipeline<>(result);
    }

    /**
     * Aplica una transformación síncrona al valor exitoso.
     */
    public <U> SyncResultPipeline<U, E> map(Function<T, U> mapper) {
        if (!result.isSuccess()) {
            return new SyncResultPipeline<>(Result.failure(result.getError()));
        }
        return new SyncResultPipeline<>(Result.success(mapper.apply(result.getValue())));
    }

    /**
     * Encadena una transformación que retorna otro Result.
     */
    public <U> SyncResultPipeline<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        if (!result.isSuccess()) {
            return new SyncResultPipeline<>(Result.failure(result.getError()));
        }
        return new SyncResultPipeline<>(mapper.apply(result.getValue()));
    }

    /**
     * Ejecuta un efecto colateral si el resultado es exitoso.
     */
    public SyncResultPipeline<T, E> peek(Consumer<T> action) {
        if (result.isSuccess()) {
            action.accept(result.getValue());
        }
        return this;
    }

    /**
     * Ejecuta una validación, convirtiendo a error si no pasa.
     */
    public SyncResultPipeline<T, E> validate(Function<T, ValidationResult<T>> validator, Function<String, E> errorMapper) {
        if (!result.isSuccess()) {
            return this;
        }
        ValidationResult<T> validation = validator.apply(result.getValue());
        if (validation.isValid()) {
            return new SyncResultPipeline<>(Result.success(validation.getValue()));
        }
        return new SyncResultPipeline<>(Result.failure(errorMapper.apply(validation.getErrors().getFirst())));
    }

    /**
     * Filtra el valor exitoso según un predicado. Si no se cumple, se convierte en error.
     */
    public SyncResultPipeline<T, E> filter(Predicate<T> predicate, E error) {
        if (result.isSuccess() && !predicate.test(result.getValue())) {
            return new SyncResultPipeline<>(Result.failure(error));
        }
        return this;
    }

    /**
     * Ejecuta una acción si el resultado es exitoso.
     */
    public SyncResultPipeline<T, E> onSuccess(Consumer<T> action) {
        return peek(action);
    }

    /**
     * Ejecuta una acción si el resultado es un error.
     */
    public SyncResultPipeline<T, E> onFailure(Consumer<E> errorConsumer) {
        if (!result.isSuccess()) {
            errorConsumer.accept(result.getError());
        }
        return this;
    }

    /**
     * Permite recuperar el valor si hubo error.
     */
    public SyncResultPipeline<T, E> recover(Function<E, T> fallbackFunction) {
        if (result.isSuccess()) {
            return this;
        }
        return new SyncResultPipeline<>(Result.success(fallbackFunction.apply(result.getError())));
    }

    /**
     * Finaliza la pipeline y retorna el resultado.
     */
    public Result<T, E> build() {
        return result;
    }
}