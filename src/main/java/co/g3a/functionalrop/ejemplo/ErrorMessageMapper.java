package co.g3a.functionalrop.ejemplo;

public class ErrorMessageMapper {

    public static String toUserMessage(AppError error) {
        return switch (error) {
            //case AppError.NameBlank e -> "Name must not be blank";
            case AppError.EmailBlank e -> "Email must not be blank";
            case AppError.EmailInvalid e -> "Email %s is invalid".formatted(e.email());
            // Agrega otros errores...
            default -> "Unknown error";
        };
    }
}