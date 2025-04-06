package co.g3a.functionalrop;

import co.g3a.functionalrop.Result;
import co.g3a.functionalrop.ejemplo.AppError;
import co.g3a.functionalrop.ejemplo.DatosUsuario;
import co.g3a.functionalrop.ejemplo.UseCase;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

class UseCaseTest {

    private final UseCase useCase = new UseCase(new ErrorMessageProvider("es"));

    @Test
    void consultarDatosUsuarioParalelo_exito() {
        String userId = "user123";

        CompletionStage<Result<DatosUsuario, AppError>> future =
                useCase.consultarDatosUsuarioParalelo(userId);

        Result<DatosUsuario, AppError> result = future.toCompletableFuture().join();

        assertTrue(result.isSuccess(), "Debe ser exitoso");
        DatosUsuario data = result.getValue();
        assertEquals("Nombre: Juan", data.nombre());
        assertEquals(30, data.edad());
        assertTrue(data.cuentaActiva());
    }

    @Test
    void consultarDatosUsuarioParalelo_fallaNombre() {
        UseCase useCaseError = new UseCase(new ErrorMessageProvider("es")) {
            @Override
            public CompletionStage<Result<String, AppError>> buscarNombreUsuario(String id) {
                return simulateFailure(new AppError.DbError("Nombre no encontrado"), 100);
            }
        };

        CompletionStage<Result<DatosUsuario, AppError>> future =
                useCaseError.consultarDatosUsuarioParalelo("userX");

        Result<DatosUsuario, AppError> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess(), "Debe fallar");
        assertTrue(result.getError() instanceof AppError.MultipleErrors);
        AppError.MultipleErrors errors = (AppError.MultipleErrors) result.getError();
        assertEquals(1, errors.errors().size());
        assertTrue(errors.errors().get(0) instanceof AppError.DbError);
    }

    @Test
    void consultarDatosUsuarioParalelo_fallaMultiple() {
        UseCase useCaseMultiError = new UseCase(new ErrorMessageProvider("es")) {
            @Override
            public CompletionStage<Result<String, AppError>> buscarNombreUsuario(String id) {
                return simulateFailure(new AppError.DbError("Nombre no disponible"), 50);
            }

            @Override
            public CompletionStage<Result<Boolean, AppError>> verificarCuentaActiva(String id) {
                return simulateFailure(new AppError.DbError("Cuenta suspendida"), 50);
            }
        };

        CompletionStage<Result<DatosUsuario, AppError>> future =
                useCaseMultiError.consultarDatosUsuarioParalelo("userZ");

        Result<DatosUsuario, AppError> result = future.toCompletableFuture().join();

        assertFalse(result.isSuccess(), "Debe fallar");
        assertTrue(result.getError() instanceof AppError.MultipleErrors);
        AppError.MultipleErrors errors = (AppError.MultipleErrors) result.getError();
        assertEquals(2, errors.errors().size());
    }

    // Simula un fallo controlado
    private <T> CompletionStage<Result<T, AppError>> simulateFailure(AppError error, long delay) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {}
            return Result.failure(error);
        });
    }
}