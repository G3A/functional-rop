package co.g3a.functionalrop.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Clase de utilidad para ejecutar de forma segura operaciones con efectos secundarios (funciones sin salida o dead-end functions),
 * con y sin transformación.
 * <p>
 * Admite registro estructurado y gestión consistente de errores funcionales (Result.failure).
 */
public class DeadEnd {

    private final Executor executor;

    public DeadEnd(Executor executor) {
        this.executor = executor;
    }


    // ------------------------------------------------------------------------------------------------
    // 2️⃣ Transformer function: applies a function In -> Out with side effects, wraps in Result
    // ------------------------------------------------------------------------------------------------
    public <In, Out, E> CompletionStage<Result<Out, E>> runSafeResultTransform(
            In input,
            Function<In, Result<Out, E>> function,
            Function<Throwable, E> throwableHandler
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return function.apply(input);
            } catch (Exception ex) {
                E error = throwableHandler.apply(ex);
                throw new RuntimeException(error.toString(), ex);
            }
        }, this.executor);
    }
}