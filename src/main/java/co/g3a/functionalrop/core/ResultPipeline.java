package co.g3a.functionalrop.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@code ResultPipeline} es una API fluida que permite encadenar operaciones funcionales y asíncronas
 * sobre un resultado envolvente {@link Result}, facilitando el manejo de errores, validaciones y tareas
 * concurrentes de forma declarativa.
 *
 * <p>Está diseñada para flujos en los que se desea combinar procesamiento funcional con asincronía,
 * sin perder expresividad ni control del flujo de errores.</p>
 *
 * @param <T> tipo del valor exitoso
 * @param <E> tipo del error
 */
public class ResultPipeline<T, E> {

    private final CompletionStage<Result<T, E>> result;

    /**
     * Constructor privado. Usa {@link #use(Object)} para inicializar una pipeline.
     *
     * @param result etapa asíncrona que encapsula un {@link Result}
     */
    ResultPipeline(CompletionStage<Result<T, E>> result) {
        this.result = result;
    }

    /**
     * Inicializa una pipeline con un valor exitoso.
     *
     * @param value valor inicial con el que arranca la pipeline
     * @param <T>   tipo del valor
     * @param <E>   tipo del error
     * @return instancia de {@code ResultPipeline}
     */
    public static <T, E> ResultPipeline<T, E> use(T value) {
        return new ResultPipeline<>(CompletableFuture.completedFuture(Result.success(value)));
    }

    /**
     * Ejecuta una validación sobre el valor exitoso actual.
     * Si la validación falla, convierte el pipeline en {@code Result.failure}.
     *
     * @param validator   función de validación que retorna un {@link ValidationResult}
     * @param errorMapper conversor que transforma el mensaje de error en un objeto del tipo {@code E}
     * @return pipeline actualizada según el resultado de la validación
     */
    public ResultPipeline<T, E> validate(Function<T, ValidationResult<T>> validator, Function<String, E> errorMapper) {
        CompletionStage<Result<T, E>> newResult = result.thenApply(res -> {
            if (!res.isSuccess()) return res;
            ValidationResult<T> validation = validator.apply(res.getValue());
            if (validation.isValid()) {
                return Result.success(validation.getValue());
            }
            return Result.failure(errorMapper.apply(validation.getErrors().getFirst()));
        });
        return new ResultPipeline<>(newResult);
    }

    /**
     * Transforma el valor exitoso actual utilizando una función síncrona.
     *
     * @param mapper función transformadora {@code T -> U}
     * @param <U>    nuevo tipo del valor transformado
     * @return nueva instancia de {@code ResultPipeline} con el tipo transformado
     */
    public <U> ResultPipeline<U, E> map(Function<T, U> mapper) {
        CompletionStage<Result<U, E>> newResult = result.thenApply(res -> res.map(mapper));
        return new ResultPipeline<>(newResult);
    }

    /**
     * Encadena una operación síncrona que retorna otro {@link Result}.
     *
     * @param mapper función transformadora {@code T -> Result<U, E>}
     * @param <U>    nuevo tipo del valor
     * @return nueva pipeline con la etapa encadenada
     */
    public <U> ResultPipeline<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        CompletionStage<Result<U, E>> newResult = result.thenApply(res -> {
            if (res.isSuccess()) {
                return mapper.apply(res.getValue());
            }
            return Result.failure(res.getError());
        });
        return new ResultPipeline<>(newResult);
    }

    /**
     * Encadena una operación asíncrona que retorna otro {@link Result}.
     *
     * @param mapper función transformadora asíncrona {@code T -> CompletionStage<Result<U, E>>}
     * @param <U>    nuevo tipo del valor
     * @return nueva pipeline con la etapa asíncrona encadenada
     */
    public <U> ResultPipeline<U, E> flatMapAsync(Function<T, CompletionStage<Result<U, E>>> mapper) {
        CompletionStage<Result<U, E>> newResult = result.thenCompose(res ->
                res.isSuccess()
                        ? mapper.apply(res.getValue())
                        : CompletableFuture.completedFuture(Result.failure(res.getError()))
        );
        return new ResultPipeline<>(newResult);
    }

    /**
     * Ejecuta un efecto colateral (side effect) síncrono sobre el valor exitoso de la pipeline.
     * No altera el valor ni el estado del pipeline.
     *
     * @param action consumidor a ejecutar si el resultado es exitoso
     * @return pipeline original
     */
    public ResultPipeline<T, E> peek(Consumer<T> action) {
        CompletionStage<Result<T, E>> newResult = result.thenApply(res -> {
            if (res.isSuccess()) {
                action.accept(res.getValue());
            }
            return res;
        });
        return new ResultPipeline<>(newResult);
    }

    /**
     * Ejecuta un efecto colateral (side effect) asíncrono sobre el valor exitoso de la pipeline.
     *
     * @param asyncAction función asíncrona a ejecutar si el resultado es exitoso
     * @return pipeline original
     */
    public ResultPipeline<T, E> peekAsync(Function<T, CompletionStage<Void>> asyncAction) {
        CompletionStage<Result<T, E>> newResult = result.thenCompose(res -> {
            if (res.isSuccess()) {
                return asyncAction.apply(res.getValue()).thenApply(v -> res);
            } else {
                return CompletableFuture.completedFuture(res);
            }
        });
        return new ResultPipeline<>(newResult);
    }

    /**
     * Finaliza la pipeline y retorna la etapa asíncrona con el {@link Result}.
     *
     * @return {@code CompletionStage<Result<T, E>>}
     */
    public CompletionStage<Result<T, E>> build() {
        return result;
    }

    /**
     * Permite consumir el {@link Result} final (éxito o error) una vez esté disponible.
     *
     * @param consumer consumidor del resultado final
     * @return {@code CompletionStage<Void>} indicando la finalización
     */
    public CompletionStage<Void> thenAccept(Consumer<Result<T, E>> consumer) {
        return result.thenAccept(consumer);
    }

    /**
     * Ejecuta múltiples tareas en paralelo (no tipadas) y combina sus resultados.
     *
     * @param input         valor de entrada para cada tarea
     * @param tasks         lista de funciones asíncronas
     * @param errorCombiner función para combinar múltiples errores
     * @param <T>           tipo de entrada
     * @param <E>           tipo de error
     * @return resultado con lista de objetos o error combinado
     */
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
                    List<E> errors = new ArrayList<>();
                    for (CompletableFuture<Result<?, E>> cf : cfList) {
                        try {
                            Result<?, E> res = cf.join();
                            if (res.isSuccess()) {
                                results.add(res.getValue());
                            } else {
                                errors.add(res.getError());
                            }
                        } catch (Exception ex) {
                            throw new CompletionException("Task failed", ex);
                        }
                    }
                    if (!errors.isEmpty()) {
                        return Result.failure(errorCombiner.apply(errors));
                    }
                    return Result.success(results);
                });
    }

    /**
     * Ejecuta múltiples tareas (tipadas) en paralelo y las combina.
     *
     * @param input         input común para todas las tareas
     * @param tasks         lista de funciones asíncronas que retornan {@code Result<Output, E>}
     * @param errorCombiner combinador de errores en caso de múltiples fallos
     * @param <Input>       tipo de entrada
     * @param <Output>      tipo de salida común
     * @param <E>           tipo del error
     * @return resultado final con lista de salidas o error combinado
     */
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
                        try {
                            Result<Output, E> res = future.join();
                            if (res.isSuccess()) {
                                results.add(res.getValue());
                            } else {
                                errors.add(res.getError());
                            }
                        } catch (Exception ex) {
                            throw new CompletionException("Task failed", ex);
                        }
                    }
                    if (!errors.isEmpty()) {
                        return Result.failure(errorCombiner.apply(errors));
                    }
                    return Result.success(results);
                });
    }

    /**
     * Permite recuperar un valor predeterminado si el pipeline terminó en error.
     *
     * @param recoverFunction función que convierte el error a un valor "fallback"
     * @return nueva pipeline con el valor recuperado o el valor original
     */
    public ResultPipeline<T, E> recover(Function<E, T> recoverFunction) {
        CompletionStage<Result<T, E>> newResult = result.thenApply(res ->
                res.isSuccess()
                        ? res
                        : Result.success(recoverFunction.apply(res.getError()))
        );
        return new ResultPipeline<>(newResult);
    }

    /**
     * Filtra el valor exitoso por una condición. Si no se cumple, convierte en error.
     *
     * @param predicate condición a verificar
     * @param error     valor de error si no se cumple la condición
     * @return pipeline filtrada
     */
    public ResultPipeline<T, E> filter(java.util.function.Predicate<T> predicate, E error) {
        CompletionStage<Result<T, E>> newResult = result.thenApply(res ->
                res.isSuccess() && !predicate.test(res.getValue())
                        ? Result.failure(error)
                        : res
        );
        return new ResultPipeline<>(newResult);
    }

    /**
     * Ejecuta una acción si el resultado es exitoso.
     */
    public ResultPipeline<T, E> onSuccess(Consumer<T> action) {
        return peek(action);
    }

    /**
     * Ejecuta una acción si el resultado es un error.
     */
    public ResultPipeline<T, E> onFailure(Consumer<E> errorConsumer) {
        CompletionStage<Result<T, E>> newResult = result.thenApply(res -> {
            if (!res.isSuccess()) {
                errorConsumer.accept(res.getError());
            }
            return res;
        });
        return new ResultPipeline<>(newResult);
    }

    /**
     * Permite combinar el valor o el error en un único resultado final, de forma asíncrona.
     *
     * @param onFailure función para error
     * @param onSuccess función para éxito
     * @param <U>       tipo del valor final
     * @return etapa asíncrona con valor resuelto
     */
    public <U> CompletionStage<U> foldAsync(Function<E, U> onFailure, Function<T, U> onSuccess) {
        return result.thenApply(res -> res.fold(onFailure, onSuccess));
    }
}