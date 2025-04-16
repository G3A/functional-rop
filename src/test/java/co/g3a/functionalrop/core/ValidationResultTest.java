package co.g3a.functionalrop.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void valid_should_contain_value_and_no_errors() {
        ValidationResult<String> result = ValidationResult.valid("Hola");

        assertTrue(result.isValid());
        assertEquals("Hola", result.getValue());

        assertThrows(IllegalStateException.class, result::getErrors);
    }

    @Test
    void invalid_should_contain_errors_and_no_value() {
        ValidationResult<String> result = ValidationResult.invalid("Campo requerido");

        assertFalse(result.isValid());
        assertEquals(List.of("Campo requerido"), result.getErrors());

        assertThrows(IllegalStateException.class, result::getValue);
    }

    @Test
    void map_should_transform_valid_value() {
        ValidationResult<Integer> result = ValidationResult.valid(10)
                .map(i -> i * 2);

        assertTrue(result.isValid());
        assertEquals(20, result.getValue());
    }

    @Test
    void map_should_preserve_errors_if_invalid() {
        ValidationResult<Integer> result = ValidationResult.<Integer>invalid("Edad inválida")
                .map(i -> i * 2);

        assertFalse(result.isValid());
        assertEquals(List.of("Edad inválida"), result.getErrors());
    }

    @Test
    void combine_should_return_first_valid_if_no_errors() {
        var r1 = ValidationResult.valid("Juan");
        var r2 = ValidationResult.valid("Pedro");

        ValidationResult<String> combined = ValidationResult.combine(List.of(r1, r2));

        assertTrue(combined.isValid());
        assertEquals("Juan", combined.getValue()); // se queda con el primero
    }

    @Test
    void combine_should_return_invalid_if_any_has_errors() {
        var valid = ValidationResult.valid("Carlos");
        var invalid1 = ValidationResult.<String>invalid("Nombre vacío");
        var invalid2 = ValidationResult.<String>invalid("Edad negativa");

        ValidationResult<String> combined = ValidationResult.combine(List.of(valid, invalid1, invalid2));

        assertFalse(combined.isValid());
        assertEquals(List.of("Nombre vacío", "Edad negativa"), combined.getErrors());
    }
}