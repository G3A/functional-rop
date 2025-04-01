package co.g3a.functionalrop.ejemplo;

import co.g3a.functionalrop.*;
import co.g3a.functionalrop.logging.ConsoleStructuredLogger;
import co.g3a.functionalrop.logging.StructuredLogger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.List;

public class UseCase {

    private final ErrorMessageProvider messages;
    private final StructuredLogger logger = new ConsoleStructuredLogger();

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

    public CompletionStage<Result<String>> executeUseCase(Request request) {
        ValidationResult<Request> validation = validateRequest(request);
        if (!validation.isValid()) {
            return CompletableFuture.completedFuture(Result.failure(String.join(", ", validation.getErrors())));
        }

        Request canonical = canonicalizeEmail(validation.getValue());

        return updateDb(canonical)
            .thenCompose(res -> res.flatMapAsync(this::sendEmail))
            .thenApply(res -> res.map(r -> "Success: " + r.email));
    }



    public ValidationResult<Request> validateRequest(Request r) {
        List<ValidationResult<Void>> validations = List.of(
                validateNotEmpty(r.email, messages.get("empty_email")),
                validateEmailFormat(r.email, messages.get("invalid_email")),
                validateMinLength(r.name, 3, messages.get("short_name")),
                validateNotEmpty(r.password, messages.get("empty_password")),
                validateMinLength(r.password, 8, messages.get("short_password")),
                validateMinAge(r.age, 18, messages.get("underage"))
        );

        return ValidationResult.combine(validations)
                .map(x -> r); // devolver el request si todo fue válido
    }
    private ValidationResult<Void> validateNotEmpty(String value, String errorMsg) {
        return (value == null || value.trim().isEmpty())
                ? ValidationResult.invalid(errorMsg)
                : ValidationResult.valid(null);
    }

    private ValidationResult<Void> validateMinLength(String value, int minLength, String errorMsg) {
        return (value == null || value.length() < minLength)
                ? ValidationResult.invalid(errorMsg)
                : ValidationResult.valid(null);
    }

    private ValidationResult<Void> validateEmailFormat(String email, String errorMsg) {
        return (!email.contains("@") || !email.contains("."))
                ? ValidationResult.invalid(errorMsg)
                : ValidationResult.valid(null);
    }

    private ValidationResult<Void> validateMinAge(int age, int minAge, String errorMsg) {
        return (age < minAge)
                ? ValidationResult.invalid(errorMsg)
                : ValidationResult.valid(null);
    }

    /**
     *  Single track function: Trim spaces and lowercase
     *  A simple function that doesn't generate errors – a "one-track" function
     */
    public Request canonicalizeEmail(Request r) {
        r.email = r.email.trim().toLowerCase();
        return r;
    }


    /**
     * Dead-end function: performs DB update as a side effect.
     * Doesn't transform the input but returns it wrapped in Result.
     * Only fails if side effect fails (e.g., DB error), preserving the pipeline.
     * A function that doesn't return anything– a "dead-end" function.
     */
    public CompletionStage<Result<Request>> updateDb(Request r) {
        return DeadEnd.runSafe(
                r,
                req -> {
                    System.out.println("🗃️ Guardando en base de datos: " + req.email);
                    sleep(100);
                },
                "DB error",
                "update_db",
                logger
        );
    }

    /**
     * Functions that throw exceptions
     *
     * Dead-end function: sends an email as a side-effect.
     * Does not modify input, returns it in a Result.
     * Exceptions are caught and returned as Failure.
     * 🧙‍♂️ "Do or do not. There is no try." – Yoda
     */
    public CompletionStage<Result<Request>> sendEmail(Request r) {
        return DeadEnd.runSafe(
                r,
                req -> {
                    System.out.println("📧 Enviando email a: " + req.email);
                    if (req.email.contains("fail")) {
                        throw new RuntimeException("SMTP error");
                    }
                    sleep(100);
                },
                "SendEmail error",
                "send_email",
                logger
        );
    }

    public CompletionStage<Result<String>> generateActivationCode(Request r) {
        return DeadEnd.runSafeTransform(
                r,
                req -> {
                    if (req.email.contains("@example.com")) {
                        throw new IllegalArgumentException("Domain not allowed");
                    }
                    // Simula creación de código único
                    return req.name.substring(0, 2).toUpperCase() + "-" + System.currentTimeMillis();
                },
                "Activation code error",
                "generate_activation_code",
                logger
        );
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    public static void main(String[] args) {
        var provider = new ErrorMessageProvider("es"); // Cambia a "en" para inglés
        UseCase useCase = new UseCase(provider);

        //alternativaDeUso1(useCase);
        //useCase.alternativaDeUso2();
        useCase.alternativaDeUso3();
        
    }

    private static void alternativaDeUso1(UseCase uc) {
        var request = new Request(
                "  TEST@ejemplo.COM ",    // email
                "Juan Pérez",             // name
                "superpassword",          // password
                30                        // age
        );

        uc.executeUseCase(request)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        System.out.println("✅ Resultado: " + result.getValue());
                    } else {
                        System.out.println("❌ Error: " + result.getError());
                    }
                });

        try { Thread.sleep(1000); } catch (Exception ignored) { }
    }

    //✅ Ejemplo de uso fluido
    private void alternativaDeUso2() {
        var request = new Request(
                "  TEST@ejemplo.COM ",    // email
                "Juan Pérez",             // name
                "superpassword",          // password
                30                        // age
        );

        ResultPipeline.use(request)
                .validate(this::validateRequest)
                .map(this::canonicalizeEmail)
                .flatMapAsync(this::updateDb)
                .flatMapAsync(this::sendEmail)
                .map(r -> "Success")
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        System.out.println("✅ Resultado fluido: " + result.getValue());
                    } else {
                        System.out.println("❌ Error fluido: " + result.getError());
                    }
                });

        try { Thread.sleep(1000); } catch (Exception ignored) { }
    }

    //🧠 Alternativa simplificada con ResultPipeline.use()
    //✅ Ejemplo de uso fluido mejor aún:
    private void alternativaDeUso3() {
        /*
        var request = new Request(
                "  TEST@ejemplo.COM ", // email
                "Jo",                  // name (demasiado corto)
                "123456",              // password (corta)
                16                     // edad (menor)
        );

         */
        var request = new Request(
                "  TEST@ejemplo.COM ",    // email
                "Juan Pérez",             // name
                "superpassword",          // password
                30                        // age
        );
        ResultPipeline.use(request)
                .validate(this::validateRequest)
                .map(this::canonicalizeEmail)
                .flatMapAsync(this::updateDb)      // Dead-end
                .flatMapAsync(this::sendEmail)     // Dead-end + catch exceptions
                .flatMapAsync(this::generateActivationCode) // Devuelve CompletionStage<Result<String>>
                .map(r -> "Success")
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        System.out.println("✅ Resultado: " + result.getValue());
                    } else {
                        System.out.println("❌ Error: " + result.getError());
                    }
                });

        try { Thread.sleep(1000); } catch (Exception ignored) { }
    }


}