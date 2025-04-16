package co.g3a.functionalrop.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ResultPipelineTest {

    @Test
    void use_should_create_success_pipeline() throws Exception {
        Result<Integer, String> result = ResultPipeline.<Integer, String>use(42)
                .build()
                .toCompletableFuture()
                .get();

        assertTrue(result.isSuccess());
        assertEquals(42, result.getValue());
    }

    @Test
    void map_should_transform_value() throws Exception {
        Result<String, String> result = ResultPipeline.<Integer, String>use(10)
                .map(i -> "Valor: " + i)
                .build()
                .toCompletableFuture()
                .get();

        assertTrue(result.isSuccess());
        assertEquals("Valor: 10", result.getValue());
    }

    @Test
    void flatMapAsync_should_chain_async_function() throws Exception {
        Result<String, String> result = ResultPipeline.<Integer, String>use(5)
                .flatMapAsync(i -> CompletableFuture.completedFuture(Result.success("Nº: " + i)))
                .build()
                .toCompletableFuture()
                .get();

        assertTrue(result.isSuccess());
        assertEquals("Nº: 5", result.getValue());
    }

    @Test
    void peekAsync_should_execute_side_effect_on_success() throws Exception {
        AtomicReference<String> observed = new AtomicReference<>();

        Result<String, String> result = ResultPipeline.<String, String>use("ABC")
                .peekAsync(value -> {
                    observed.set("Visto: " + value);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .toCompletableFuture()
                .get();

        assertTrue(result.isSuccess());
        assertEquals("ABC", result.getValue());
        assertEquals("Visto: ABC", observed.get());
    }

    @Test
    void peekAsync_should_be_skipped_on_failure() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);

        ResultPipeline<String, String> pipeline = new ResultPipeline<>(
                CompletableFuture.completedFuture(Result.failure("ERROR"))
        );

        Result<String, String> result = pipeline
                .peekAsync(value -> {
                    called.set(true);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .toCompletableFuture()
                .get();

        assertFalse(result.isSuccess());
        assertEquals("ERROR", result.getError());
        assertFalse(called.get());
    }

    @Test
    void runInParallel_should_return_combined_success() throws Exception {
        List<Function<String, CompletionStage<Result<?, String>>>> tasks = List.of(
                s -> CompletableFuture.completedFuture(Result.success(s + "1")),
                s -> CompletableFuture.completedFuture(Result.success(s + "2"))
        );

        Result<List<Object>, String> result = ResultPipeline.runInParallel(
                "T", tasks, errs -> String.join(",", errs)
        ).toCompletableFuture().get();

        assertTrue(result.isSuccess());
        List<Object> values = result.getValue();
        assertEquals(2, values.size());
        assertEquals("T1", values.get(0));
        assertEquals("T2", values.get(1));
    }

    @Test
    void runInParallel_should_collect_errors() throws Exception {
        List<Function<String, CompletionStage<Result<?, String>>>> tasks = List.of(
                s -> CompletableFuture.completedFuture(Result.failure("Err1")),
                s -> CompletableFuture.completedFuture(Result.failure("Err2"))
        );

        Result<List<Object>, String> result = ResultPipeline.runInParallel(
                "T", tasks, errs -> String.join(" | ", errs)
        ).toCompletableFuture().get();

        assertFalse(result.isSuccess());
        assertEquals("Err1 | Err2", result.getError());
    }

    @Test
    void runInParallelTyped_should_return_typed_results() throws Exception {
        List<Function<String, CompletionStage<Result<String, String>>>> tasks = List.of(
                s -> CompletableFuture.completedFuture(Result.success(s + "A")),
                s -> CompletableFuture.completedFuture(Result.success(s + "B"))
        );

        Result<List<String>, String> result = ResultPipeline.runInParallelTyped(
                "X", tasks, errs -> String.join(",", errs)
        ).toCompletableFuture().get();

        assertTrue(result.isSuccess());
        List<String> values = result.getValue();
        assertEquals(List.of("XA", "XB"), values);
    }

    @Test
    void runInParallelTyped_should_collect_errors() throws Exception {
        List<Function<String, CompletionStage<Result<String, String>>>> tasks = List.of(
                s -> CompletableFuture.completedFuture(Result.failure("X")),
                s -> CompletableFuture.completedFuture(Result.failure("Y"))
        );

        Result<List<String>, String> result = ResultPipeline.runInParallelTyped(
                "Z", tasks, errs -> String.join(",", errs)
        ).toCompletableFuture().get();

        assertFalse(result.isSuccess());
        assertEquals("X,Y", result.getError());
    }

    @Test
    void thenAccept_should_process_result() throws Exception {
        AtomicReference<String> actual = new AtomicReference<>();

        ResultPipeline.use("data")
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        actual.set("OK: " + result.getValue());
                    }
                })
                .toCompletableFuture()
                .get();

        assertEquals("OK: data", actual.get());
    }
}