package co.g3a.functionalrop.ejemplo.errorhandling.handler;

import co.g3a.functionalrop.ejemplo.errorhandling.builder.ApiErrorResponseBuilder;
import co.g3a.functionalrop.ejemplo.errorhandling.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ApiErrorResponseBuilder errorBuilder;

    public GlobalExceptionHandler(ApiErrorResponseBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        ApiErrorResponse response = errorBuilder.build(
                "error",
                "Falló la actualización del email asociado al usuario", // o un mensaje más genérico
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request,
                List.of(new ApiErrorResponse.ErrorDetail("INTERNAL_ERROR", "Ocurrió un error inesperado. Intente más tarde.")),
                recuperarUserId(), // desde contexto
                recuperarSessionId() // desde contexto/cookie
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String recuperarUserId() {
        // Aquí deberías usar SecurityContext, token, etc.
        return "USR-32212";
    }

    private String recuperarSessionId() {
        // Lo mismo, si tienes sesión u otras fuentes
        return "abc123";
    }
}