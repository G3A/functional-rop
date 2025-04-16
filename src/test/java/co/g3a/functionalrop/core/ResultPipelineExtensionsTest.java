package co.g3a.functionalrop.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ResultPipelineExtensionsTest {

    @Test
    void recover_should_return_fallback_on_failure() throws Exception {
        ResultPipeline<String, String> pipeline = new ResultPipeline<>(
                CompletableFuture.completedFuture(Result.failure("ERROR"))
        );

        String value = pipeline
                .recover(error -> "Recovered")
                .build()
                .toCompletableFuture()
                .get()
                .getValue();

        assertEquals("Recovered", value);
    }

    @Test
    void recover_should_preserve_success_value() throws Exception {
        String value = ResultPipeline.<String, String>use("OK")
                .recover(error -> "Fallback")
                .build()
                .toCompletableFuture()
                .get()
                .getValue();

        assertEquals("OK", value);
    }

    @Test
    void filter_should_allow_value_if_predicate_passes() throws Exception {
        boolean result = ResultPipeline.<Integer, String>use(10)
                .filter(i -> i > 5, "Too small")
                .build()
                .toCompletableFuture()
                .get()
                .isSuccess();

        assertTrue(result);
    }

    @Test
    void filter_should_fail_if_predicate_fails() throws Exception {
        Result<Integer, String> result = ResultPipeline.<Integer, String>use(3)
                .filter(i -> i > 5, "Too small")
                .build()
                .toCompletableFuture()
                .get();

        assertFalse(result.isSuccess());
        assertEquals("Too small", result.getError());
    }

    @Test
    void onSuccess_should_be_called_on_success() throws Exception {
        AtomicReference<String> called = new AtomicReference<>();
        ResultPipeline.<String, String>use("Hola")
                .onSuccess(val -> called.set("OK: " + val))
                .build()
                .toCompletableFuture()
                .get();

        assertEquals("OK: Hola", called.get());
    }

    @Test
    void onFailure_should_be_called_on_error() throws Exception {
        AtomicReference<String> called = new AtomicReference<>();
        new ResultPipeline<>(CompletableFuture.completedFuture(Result.failure("FAIL")))
                .onFailure(err -> called.set("ERR: " + err))
                .build()
                .toCompletableFuture()
                .get();

        assertEquals("ERR: FAIL", called.get());
    }

    @Test
    void foldAsync_should_resolve_to_success_path() throws Exception {
        String message = ResultPipeline.<String, String>use("Hola")
                .foldAsync(err -> "Error: " + err, ok -> "OK: " + ok)
                .toCompletableFuture()
                .get();

        assertEquals("OK: Hola", message);
    }

    @Test
    void foldAsync_should_resolve_to_failure_path() throws Exception {
        String message = new ResultPipeline<>(CompletableFuture.completedFuture(Result.failure("Oops")))
                .foldAsync(err -> "Error: " + err, ok -> "OK: " + ok)
                .toCompletableFuture()
                .get();

        assertEquals("Error: Oops", message);
    }
}