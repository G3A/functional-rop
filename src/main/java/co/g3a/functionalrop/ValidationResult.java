package co.g3a.functionalrop;

import java.util.List;
import java.util.function.Function;

/**
 * Representa el resultado de una validación: puede ser válido (un valor)
 * o inválido (una lista de errores).
 */
public sealed interface ValidationResult<T>
        permits ValidationResult.Valid, ValidationResult.Invalid {

    boolean isValid();
    T getValue();
    List<String> getErrors();

    // 🟢 Factories
    static <T> ValidationResult<T> valid(T value) {
        return new Valid<>(value);
    }

    static <T> ValidationResult<T> invalid(List<String> errors) {
        return new Invalid<>(errors);
    }

    static <T> ValidationResult<T> invalid(String error) {
        return new Invalid<>(List.of(error));
    }

    // 🧠 Transformar valor si es válido
    default <U> ValidationResult<U> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Valid<T> v -> valid(mapper.apply(v.value()));
            case Invalid<T> i -> invalid(i.errors());
        };
    }

    // ➕ Combinar múltiples validaciones y acumular errores
    static <T> ValidationResult<T> combine(List<ValidationResult<T>> results) {
        List<String> allErrors = results.stream()
                .filter(r -> !r.isValid())
                .flatMap(r -> r.getErrors().stream())
                .toList();

        return allErrors.isEmpty()
                ? results.getFirst()
                : invalid(allErrors);
    }

    // ✅ Resultado válido
    record Valid<T>(T value) implements ValidationResult<T> {
        @Override public boolean isValid() { return true; }

        @Override public T getValue() { return value; }

        @Override public List<String> getErrors() {
            throw new IllegalStateException("Valid does not contain errors");
        }
    }

    // ❌ Resultado inválido
    record Invalid<T>(List<String> errors) implements ValidationResult<T> {
        @Override public boolean isValid() { return false; }

        @Override public T getValue() {
            throw new IllegalStateException("Invalid does not contain a value");
        }

        @Override public List<String> getErrors() { return errors; }
    }
}