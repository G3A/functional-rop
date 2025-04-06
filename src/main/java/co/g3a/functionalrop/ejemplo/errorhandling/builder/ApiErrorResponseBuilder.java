package co.g3a.functionalrop.ejemplo.errorhandling.builder;

import co.g3a.functionalrop.ejemplo.errorhandling.model.ApiErrorResponse;
import co.g3a.functionalrop.ejemplo.AppError;
import co.g3a.functionalrop.ejemplo.ErrorMessageMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ApiErrorResponseBuilder {

    private final ErrorMessageMapper mapper;
    private final String environment = "production";
    private final String service = "user-service";

    public ApiErrorResponseBuilder(ErrorMessageMapper mapper) {
        this.mapper = mapper;
    }

    public ApiErrorResponse build(
            String level,
            String message,
            int status,
            HttpServletRequest request,
            List<ApiErrorResponse.ErrorDetail> errors,
            String userId,
            String sessionId
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                level,
                message,
                MDC.get("correlationId"),
                MDC.get("traceId"),
                userId,
                sessionId,
                environment,
                service,
                status,
                request.getRequestURI(),
                errors
        );
    }

    public ApiErrorResponse buildSimple(
            String message,
            int status,
            HttpServletRequest request,
            String code,
            String details
    ) {
        var errorList = List.of(new ApiErrorResponse.ErrorDetail(code, details));
        return build("error", message, status, request, errorList, null, null);
    }

    public ResponseEntity<Object> from(AppError error, HttpServletRequest request, String userId) {
        int status = mapToStatus(error);
        String message = "Ocurrió un error en la solicitud";

        List<ApiErrorResponse.ErrorDetail> details = switch (error) {
            case AppError.MultipleErrors multi -> multi.errors().stream()
                    .map(this::toErrorDetail)
                    .toList();
            default -> List.of(toErrorDetail(error));
        };

        ApiErrorResponse response = build(
                "error",
                message,
                status,
                request,
                details,
                userId,
                null
        );

        return ResponseEntity.status(status).body(response);
    }

    public ResponseEntity<Object> from(AppError error, HttpServletRequest request) {
        return from(error, request, null);
    }

    private ApiErrorResponse.ErrorDetail toErrorDetail(AppError error) {
        String code = mapAppErrorToCode(error);
        String message = mapper.toUserMessage(error);
        return new ApiErrorResponse.ErrorDetail(code, message);
    }

    private int mapToStatus(AppError error) {
        return switch (error) {
            case AppError.NameBlank e -> 400;
            case AppError.EmailBlank e -> 400;
            case AppError.EmailInvalid e -> 400;
            case AppError.PasswordBlank e -> 400;
            case AppError.PasswordTooShort e -> 400;
            case AppError.NameTooShort e -> 400;
            case AppError.UnderAge e -> 400;
            case AppError.MultipleErrors e -> 400;

            case AppError.DbError e -> 500;
            case AppError.EmailSendError e -> 500;
            case AppError.ActivationCodeError e -> 500;
        };
    }

    private String mapAppErrorToCode(AppError error) {
        return switch (error) {
            case AppError.NameBlank __         -> "NAME_BLANK";
            case AppError.EmailBlank __        -> "EMAIL_BLANK";
            case AppError.EmailInvalid __      -> "EMAIL_INVALID";
            case AppError.PasswordBlank __     -> "PASSWORD_BLANK";
            case AppError.PasswordTooShort __  -> "PASSWORD_TOO_SHORT";
            case AppError.NameTooShort __      -> "NAME_TOO_SHORT";
            case AppError.UnderAge __          -> "UNDERAGE";
            case AppError.DbError __           -> "DB_ERROR";
            case AppError.EmailSendError __    -> "EMAIL_SEND_ERROR";
            case AppError.ActivationCodeError __ -> "ACTIVATION_CODE_ERROR";
            case AppError.MultipleErrors __    -> "MULTIPLE_ERRORS";
        };
    }
}