package co.g3a.functionalrop.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Fluent API para encadenar operaciones asíncronas sobre {@link Result}.
 * <p>
 * Simplifica validaciones, mapeos y ejecución paralela de tareas.
 *
 * @param <T> tipo del valor exitoso
 * @param <E> tipo del error
 */
public class ResultPipeline<T, E> {

    private final CompletionStage<Result<T, E>> result;

    /**
     * Constructor privado. Usa {@link #use(Object)} para iniciar.
     */
    private ResultPipeline(CompletionStage<Result<T, E>> result) {
        this.result = result;
    }

    /**
     * Inicia una pipeline con un valor exitoso.
     *
     * @param value valor inicial
     * @param <T>   tipo del valor
     * @param <E>   tipo del error (no usado aún)
     * @return instancia de ResultPipeline
     */
    public static <T, E> ResultPipeline<T, E> use(T value) {
        return new ResultPipeline<>(CompletableFuture.completedFuture(Result.success(value)));
    }

    /**
     * Valida el valor actual usando una función de validación.
     * <p>
     * Si la validación falla, se convierte en un Result.failure.
     *
     * @param validator   función que retorna ValidationResult
     * @param errorMapper función para convertir error de validación -> E
     * @return misma pipeline (éxito o ahora fracaso)
     */
    public ResultPipeline<T, E> validate(Function<T, ValidationResult<T>> validator, Function<String, E> errorMapper) {
        CompletionStage<Result<T, E>> newResult = result.thenApply(res -> {
            if (!res.isSuccess()) return res;
            ValidationResult<T> validation = validator.apply(res.getValue());
            if (validation.isValid()) {
                return Result.failure(errorMapper.apply(validation.getErrors().getFirst()));
            }
            return Result.success(validation.getValue());
        });
        return new ResultPipeline<>(newResult);
    }

    /**
     * Transforma el valor exitoso (si existe) usando una función síncrona.
     *
     * @param mapper función T -> U
     * @param <U>    nuevo tipo de valor
     * @return nueva pipeline con el valor transformado (o mismo error)
     */
    public <U> ResultPipeline<U, E> map(Function<T, U> mapper) {
        CompletionStage<Result<U, E>> newResult = result.thenApply(res -> res.map(mapper));
        return new ResultPipeline<>(newResult);
    }

    /**
     * Encadena una operación asíncrona que retorna otro Result.
     *
     * @param mapper función T -> CompletionStage&lt;Result&lt;U, E&gt;&gt;
     * @param <U>    tipo del nuevo valor
     * @return pipeline con la siguiente etapa asíncrona
     */
    public <U> ResultPipeline<U, E> flatMapAsync(Function<T, CompletionStage<Result<U, E>>> mapper) {
        CompletionStage<Result<U, E>> newResult = result.thenCompose(res ->
                res.isSuccess() ? mapper.apply(res.getValue()) : CompletableFuture.completedFuture(Result.failure(res.getError()))
        );
        return new ResultPipeline<>(newResult);
    }

    /**
     * Construye el CompletionStage final.
     *
     * @return etapa asíncrona con el Result&lt;T, E&gt; final
     */
    public CompletionStage<Result<T, E>> build() {
        return result;
    }

    /**
     * Añade un consumidor final para manejar el resultado cuando esté listo.
     *
     * @param consumer función que procesa el Result (éxito o fracaso)
     * @return etapa asíncrona void (termina cuando se procese el resultado)
     */
    public CompletionStage<Void> thenAccept(java.util.function.Consumer<Result<T, E>> consumer) {
        return result.thenAccept(consumer);
    }

    /**
     * Ejecuta N tareas en paralelo, combinando sus resultados/excepciones.
     * <p>
     * Si todas acaban bien, retorna lista de resultados. Si alguna falla,
     * combina los errores usando {@code errorCombiner}.
     *
     * @param input         valor de entrada para todas las tareas
     * @param tasks         lista de funciones asíncronas (In -> Result&lt;?, E&gt;)
     * @param errorCombiner función para fusionar errores (List&lt;E&gt; -> E)
     * @param <T>           tipo de entrada
     * @param <E>           tipo de error
     * @return CompletionStage con lista de resultados o error combinado
     */
    public static <T, E> CompletionStage<Result<List<Object>, E>> runInParallel(
            T input, List<Function<T, CompletionStage<Result<?, E>>>> tasks,
            Function<List<E>, E> errorCombiner) {
        List<CompletionStage<Result<?, E>>> futures = tasks.stream()
                .map(task -> task.apply(input))
                .toList();

        List<CompletableFuture<Result<?, E>>> cfList = futures.stream()
                .map(CompletionStage::toCompletableFuture)
                .toList();

        return CompletableFuture.allOf(cfList.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<Object> results = new ArrayList<>();
                    List<E> errors = new ArrayList<>();
                    for (CompletableFuture<Result<?, E>> cf : cfList) {
                        Result<?, E> res = cf.join();
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

    /**
     * Versión tipada de {@link #runInParallel(Object, List, Function)}.
     * <p>
     * Todas las tareas retornan el mismo tipo de resultado (Output).
     *
     * @param input         entrada común
     * @param tasks         lista de tareas (In -> Result&lt;Output, E&gt;)
     * @param errorCombiner combina errores
     * @param <Input>       tipo de entrada
     * @param <Output>      tipo de salida común
     * @param <E>           tipo de error
     * @return CompletionStage con lista de Output o error combinado
     */
    public static <Input, Output, E> CompletionStage<Result<List<Output>, E>> runInParallelTyped(
            Input input, List<Function<Input, CompletionStage<Result<Output, E>>>> tasks,
            Function<List<E>, E> errorCombiner) {
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