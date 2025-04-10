package co.g3a.functionalrop;


import co.g3a.functionalrop.ejemplo.AppError;
import co.g3a.functionalrop.ejemplo.UseCase;
import org.junit.jupiter.api.Test;


import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

public class UseCasePipelineTest {

    private final ErrorMessageProvider provider = new ErrorMessageProvider("es");

    private final UseCase useCase = new UseCase(provider);

    @Test
    void flujoCompleto_exito() {
        var request = new UseCase.Request(
                "correcto@dominio.com",
                "Juan Pérez",
                "passwordSegura123",
                30
        );

        CompletionStage<Result<String, AppError>> result = ResultPipeline
                .<UseCase.Request, AppError>use(request)
                .validate(useCase::validateRequest, useCase::mapValidationToAppError)
                .map(useCase::canonicalizeEmail)
                .flatMapAsync(useCase::updateDb)
                .flatMapAsync(useCase::sendEmail)
                .flatMapAsync(useCase::generateActivationCode)
                .map(r -> "Success")
                .build();

        Result<String, AppError> finalResult = result.toCompletableFuture().join();
        assertTrue(finalResult.isSuccess());
        assertEquals("Success", finalResult.getValue());
    }

    @Test
    void flujo_fallaValidacion() {
        var request = new UseCase.Request(
                "malformado.com", // email inválido
                "JP",
                "123",
                15
        );

        CompletionStage<Result<String, AppError>> result = ResultPipeline
                .<UseCase.Request, AppError>use(request)
                .validate(useCase::validateRequest, useCase::mapValidationToAppError)
                .map(useCase::canonicalizeEmail)
                .flatMapAsync(useCase::updateDb)
                .flatMapAsync(useCase::sendEmail)
                .flatMapAsync(useCase::generateActivationCode)
                .map(r -> "Success")
                .build();

        Result<String, AppError> finalResult = result.toCompletableFuture().join();
        assertFalse(finalResult.isSuccess());
        assertTrue(finalResult.getError() instanceof AppError); // Validación fallida
    }

    @Test
    void flujo_fallaEnvioEmail() {
        var request = new UseCase.Request(
                "fail@dominio.com", // Contiene "fail", dispara excepción en sendEmail
                "Juan Pérez",
                "passwordSegura123",
                30
        );

        CompletionStage<Result<String, AppError>> result = ResultPipeline
                .<UseCase.Request, AppError>use(request)
                .validate(useCase::validateRequest, useCase::mapValidationToAppError)
                .map(useCase::canonicalizeEmail)
                .flatMapAsync(useCase::updateDb)
                .flatMapAsync(useCase::sendEmail)
                .flatMapAsync(useCase::generateActivationCode)
                .map(r -> "Success")
                .build();

        Result<String, AppError> finalResult = result.toCompletableFuture().join();
        assertFalse(finalResult.isSuccess());
        assertTrue(finalResult.getError() instanceof AppError.EmailSendError);
    }

    @Test
    void flujo_fallaEnActivationCode() {
        var request = new UseCase.Request(
                "usuario@example.com", // Dominio inválido para Activation
                "Juan Pérez",
                "passwordSegura123",
                30
        );

        CompletionStage<Result<String, AppError>> result = ResultPipeline
                .<UseCase.Request, AppError>use(request)
                .validate(useCase::validateRequest, useCase::mapValidationToAppError)
                .map(useCase::canonicalizeEmail)
                .flatMapAsync(useCase::updateDb)
                .flatMapAsync(useCase::sendEmail)
                .flatMapAsync(useCase::generateActivationCode)
                .map(r -> "Success")
                .build();

        Result<String, AppError> finalResult = result.toCompletableFuture().join();
        assertFalse(finalResult.isSuccess());
        assertTrue(finalResult.getError() instanceof AppError.ActivationCodeError);
    }
}