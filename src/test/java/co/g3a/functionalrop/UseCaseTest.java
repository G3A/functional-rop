package co.g3a.functionalrop;

import co.g3a.functionalrop.ejemplo.UseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UseCaseTest {

    ErrorMessageProvider messages = new ErrorMessageProvider("es");
    UseCase uc = new UseCase(messages);

    @Test
    void testValidRequest() {
        var request = new UseCase.Request(
                "test@example.com",
                "Juan Pérez",
                "supersecure123",
                25
        );
        ValidationResult<UseCase.Request> result = uc.validateRequest(request);
        assertTrue(result.isValid());
    }

    @Test
    void testInvalidEmail() {
        var request = new UseCase.Request(
                "bademail",
                "Juan Pérez",
                "supersecure123",
                25
        );
        var result = uc.validateRequest(request);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(messages.get("invalid_email")));
    }

    @Test
    void testEmptyEmail() {
        var request = new UseCase.Request(
                "   ",
                "Juan Pérez",
                "supersecure123",
                25
        );
        var result = uc.validateRequest(request);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(messages.get("empty_email")));
    }

    @Test
    void testShortName() {
        var request = new UseCase.Request(
                "test@example.com",
                "Jo",
                "supersecure123",
                25
        );
        var result = uc.validateRequest(request);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(messages.get("short_name")));
    }

    @Test
    void testShortPassword() {
        var request = new UseCase.Request(
                "test@example.com",
                "Juan Pérez",
                "123",
                25
        );
        var result = uc.validateRequest(request);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(messages.get("short_password")));
    }

    @Test
    void testUnderage() {
        var request = new UseCase.Request(
                "test@example.com",
                "Juan Pérez",
                "supersecure123",
                15
        );
        var result = uc.validateRequest(request);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(messages.get("underage")));
    }

    @Test
    void testMultipleErrors() {
        var request = new UseCase.Request(
                "",         // email vacío
                "Jo",       // nombre corto
                "123",      // contraseña corta
                12          // edad menor
        );
        var result = uc.validateRequest(request);
        assertFalse(result.isValid());

        var errs = result.getErrors();
        assertTrue(errs.contains(messages.get("empty_email")));
        assertTrue(errs.contains(messages.get("invalid_email")));
        assertTrue(errs.contains(messages.get("short_name")));
        assertTrue(errs.contains(messages.get("short_password")));
        assertTrue(errs.contains(messages.get("underage")));
    }
}