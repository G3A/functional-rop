package co.g3a.functionalrop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

public class ResultTest {

    @Test
    @DisplayName("🟢 Result.success debe contener valor y ser exitoso")
    void testResultSuccess() {
        Result<String, String> result = Result.success("Hola Mundo");

        assertTrue(result.isSuccess());
        assertEquals("Hola Mundo", result.getValue());
        assertThrows(UnsupportedOperationException.class, result::getError);
    }

    @Test
    @DisplayName("🔴 Result.failure debe contener error y no ser exitoso")
    void testResultFailure() {
        Result<String, String> result = Result.failure("Algo falló");

        assertFalse(result.isSuccess());
        assertEquals("Algo falló", result.getError());
        assertThrows(UnsupportedOperationException.class, result::getValue);
    }

    @Test
    @DisplayName("🧪 map transforma el valor si el resultado es success")
    void testMapOnSuccess() {
        Result<String, String> result = Result.success("abc");
        Result<Integer, String> mapped = result.map(String::length);

        assertTrue(mapped.isSuccess());
        assertEquals(3, mapped.getValue());
    }

    @Test
    @DisplayName("🚫 map no se ejecuta si es failure")
    void testMapOnFailure() {
        Result<String, String> failure = Result.failure("Fallo original");
        Result<Integer, String> mapped = failure.map(String::length);

        assertFalse(mapped.isSuccess());
        assertEquals("Fallo original", mapped.getError());
    }

    @Test
    @DisplayName("🧪 flatMap encadena otro Result si es success")
    void testFlatMapSuccess() {
        Result<String, String> result = Result.success("dato");

        Result<String, String> chained = result.flatMap(val ->
                Result.success(val.toUpperCase()));

        assertTrue(chained.isSuccess());
        assertEquals("DATO", chained.getValue());
    }

    @Test
    @DisplayName("🚫 flatMap no se ejecuta si es failure")
    void testFlatMapFailure() {
        Result<String, String> failure = Result.failure("Error inicial");

        Result<String, String> chained = failure.flatMap(val ->
                Result.success("nunca llega"));

        assertFalse(chained.isSuccess());
        assertEquals("Error inicial", chained.getError());
    }

    @Test
    @DisplayName("🧪 flatMapAsync ejecuta asincrónicamente si success")
    void testFlatMapAsync_success() {
        Result<String, String> result = Result.success("Valor base");

        CompletionStage<Result<String, String>> stage = result.flatMapAsync(val ->
                CompletableFuture.completedFuture(Result.success(val + " + async")));

        Result<String, String> finalResult = stage.toCompletableFuture().join();

        assertTrue(finalResult.isSuccess());
        assertEquals("Valor base + async", finalResult.getValue());
    }

    @Test
    @DisplayName("🚫 flatMapAsync no ejecuta si failure")
    void testFlatMapAsync_failure() {
        Result<String, String> result = Result.failure("Error async");

        CompletionStage<Result<String, String>> stage = result.flatMapAsync(val ->
                CompletableFuture.completedFuture(Result.success("No pasa")));

        Result<String, String> finalResult = stage.toCompletableFuture().join();

        assertFalse(finalResult.isSuccess());
        assertEquals("Error async", finalResult.getError());
    }
}