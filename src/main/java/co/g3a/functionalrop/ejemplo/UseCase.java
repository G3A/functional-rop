package co.g3a.functionalrop.ejemplo;

import co.g3a.functionalrop.*;
import co.g3a.functionalrop.logging.ConsoleStructuredLogger;
import co.g3a.functionalrop.logging.StructuredLogger;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.List;
import java.util.function.Function;

@Service
public class UseCase {

    private final ErrorMessageProvider messages;
    private final StructuredLogger logger = new ConsoleStructuredLogger();//Se puede indicar null si no se desea ningun tipo de logger

    public UseCase(ErrorMessageProvider messages) {
        this.messages = messages;
    }

    public static class Request {
        public String email;
        public String name;
        public String password;
        public int age;

        public Request(String email, String name, String password, int age) {
            this.email = email;
            this.name = name;
            this.password = password;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    ", password='" + password + '\'' +
                    ", age=" + age +
                    '}';
        }
    }

    public CompletionStage<Result<String, AppError>> executeUseCase(Request request) {
        ValidationResult<Request> validation = validateRequest(request);
        if (!validation.isValid()) {
            AppError error = mapValidationToAppError(validation.getErrors().get(0));
            return CompletableFuture.completedFuture(Result.failure(error));
        }

        Request canonical = canonicalizeEmail(validation.getValue());

        return updateDb(canonical)
                .thenCompose(res -> res.flatMapAsync(this::sendEmail))
                .thenCompose(res -> res.flatMapAsync(this::generateActivationCode))
                .thenApply(res -> res.map(r -> "Success"));
    }

    public ValidationResult<Request> validateRequest(Request r) {
        List<ValidationResult<Void>> validations = List.of(
                validateNotEmpty(r.email, "empty_email"),
                validateEmailFormat(r.email, "invalid_email"),
                validateMinLength(r.name, 3, "short_name"),
                validateNotEmpty(r.password, "empty_password"),
                validateMinLength(r.password, 8, "short_password"),
                validateMinAge(r.age, 18, "underage")
        );

        return ValidationResult.combine(validations).map(x -> r);
    }

    private ValidationResult<Void> validateNotEmpty(String value, String errorKey) {
        return (value == null || value.trim().isEmpty())
                ? ValidationResult.invalid(errorKey)
                : ValidationResult.valid(null);
    }

    private ValidationResult<Void> validateMinLength(String value, int minLength, String errorKey) {
        return (value == null || value.length() < minLength)
                ? ValidationResult.invalid(errorKey)
                : ValidationResult.valid(null);
    }

    private ValidationResult<Void> validateEmailFormat(String email, String errorKey) {
        return (!email.contains("@") || !email.contains("."))
                ? ValidationResult.invalid(errorKey)
                : ValidationResult.valid(null);
    }

    private ValidationResult<Void> validateMinAge(int age, int minAge, String errorKey) {
        return (age < minAge)
                ? ValidationResult.invalid(errorKey)
                : ValidationResult.valid(null);
    }

    public Request canonicalizeEmail(Request r) {
        r.email = r.email.trim().toLowerCase();
        return r;
    }

    public CompletionStage<Result<Request, AppError>> updateDb(Request r) {
        return DeadEnd.runSafe(
                r,
                req -> {
                    System.out.println("🗃️ Guardando en base de datos: " + req.email);
                    sleep(100);
                },
                ex -> new AppError.DbError("Error guardando en DB: " + ex.getMessage()),
                "update_db",
                logger
        );
    }

    public CompletionStage<Result<Request, AppError>> sendEmail(Request r) {
        return DeadEnd.runSafe(
                r,
                req -> {
                    System.out.println("📧 Enviando email a: " + req.email);
                    if (req.email.contains("fail")) throw new RuntimeException("SMTP error");
                    sleep(100);
                },
                ex -> new AppError.EmailSendError("Error al enviar email: " + ex.getMessage()),
                "send_email",
                logger
        );
    }

    public CompletionStage<Result<String, AppError>> generateActivationCode(Request r) {
        return DeadEnd.runSafeTransform(
                r,
                req -> {
                    if (req.email.contains("@example.com")) {
                        throw new IllegalArgumentException("Dominio no permitido");
                    }
                    return "AC-" + System.currentTimeMillis();
                },
                ex -> new AppError.ActivationCodeError("Fallo generando código: " + ex.getMessage()),
                "generate_activation_code",
                logger
        );
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    public AppError mapValidationToAppError(String errorMessage) {
        return switch (errorMessage) {
            case "empty_email" -> new AppError.EmailBlank();
            case "invalid_email" -> new AppError.EmailInvalid("unknown");
            case "short_name" -> new AppError.NameTooShort();
            case "empty_password" -> new AppError.PasswordBlank();
            case "short_password" -> new AppError.PasswordTooShort();
            case "underage" -> new AppError.UnderAge(0);
            default -> new AppError.DbError(errorMessage); // fallback para mensajes no mapeados
        };
    }

    public static void main(String[] args) {
        var provider = new ErrorMessageProvider("es");
        UseCase useCase = new UseCase(provider);
        useCase.alternativaDeUso3();
        useCase.ejecutarYMostrarConsultaParalela();
    }

    /**
     * As a user I want to update my name and email address
     * */
    private void alternativaDeUso3() {
        var request = new Request(
                " TEST@ejemplo.COM ",
                "Juan Pérez",
                "superpassword",
                30
        );

        ResultPipeline
                .<Request, AppError>use(request) // 👈 Aseguras tipo explícito
                .validate(this::validateRequest, this::mapValidationToAppError)
                .map(this::canonicalizeEmail)
                .flatMapAsync(this::updateDb)
                .flatMapAsync(this::sendEmail)
                .flatMapAsync(this::generateActivationCode)
                .map(r -> "Success")
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        System.out.println("✅ Resultado: " + result.getValue());
                    } else {
                        System.out.println("❌ Error: " + ErrorMessageMapper.toUserMessage(result.getError()));
                    }
                });

        try {
            Thread.sleep(1000); // Esperar async
        } catch (Exception ignored) {
        }
    }


    public void ejecutarYMostrarConsultaParalela() {
        consultarDatosUsuarioParalelo("u123")
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        DatosUsuario datos = result.getValue();
                        System.out.println("✅ Datos del usuario:");
                        System.out.println("Nombre: " + datos.nombre());
                        System.out.println("Edad: " + datos.edad());
                        System.out.println("Activa: " + datos.cuentaActiva());
                    } else {
                        System.out.println("❌ Error al consultar datos: " + ErrorMessageMapper.toUserMessage(result.getError()));
                    }
                });

        try {
            Thread.sleep(1000); // Esperar async
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Simulan llamadas a base de datos o servicios externos
    public CompletionStage<Result<String, AppError>> buscarNombreUsuario(String id) {
        return simulateSuccess("Nombre: Juan", 300);
    }

    public CompletionStage<Result<Integer, AppError>> buscarEdadUsuario(String id) {
        return simulateSuccess(30, 200);
    }

    public CompletionStage<Result<Boolean, AppError>> verificarCuentaActiva(String id) {
        return simulateSuccess(true, 100);
    }

    // Simulación genérica
    private <T> CompletionStage<Result<T, AppError>> simulateSuccess(T value, long delayMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
                return Result.success(value);
            } catch (InterruptedException e) {
                return Result.failure(new AppError.DbError("Interrupted"));
            }
        });
    }

    public CompletionStage<Result<DatosUsuario, AppError>> consultarDatosUsuarioParalelo(String userId) {
        // Encapsular cada tarea como Function<String, CompletionStage<Result<Object>>>
        Function<String, CompletionStage<Result<Object, AppError>>> nombreTask =
                id -> buscarNombreUsuario(id).thenApply(r -> r.map(v -> (Object) v));

        Function<String, CompletionStage<Result<Object, AppError>>> edadTask =
                id -> buscarEdadUsuario(id).thenApply(r -> r.map(v -> (Object) v));

        Function<String, CompletionStage<Result<Object, AppError>>> estadoTask =
                id -> verificarCuentaActiva(id).thenApply(r -> r.map(v -> (Object) v));

        List<Function<String, CompletionStage<Result<Object, AppError>>>> tasks = List.of(
                nombreTask, edadTask, estadoTask
        );

        return ResultPipeline.runInParallelTyped(
                userId,
                tasks,
                AppError.MultipleErrors::new
        ).thenApply(result -> {
            if (result.isSuccess()) {
                List<Object> values = result.getValue();

                String nombre = (String) values.get(0);
                int edad = (Integer) values.get(1);
                boolean activa = (Boolean) values.get(2);

                DatosUsuario datosUsuario = new DatosUsuario(nombre, edad, activa);
                return Result.success(datosUsuario);
            } else {
                return Result.failure(result.getError());
            }
        });
    }

}