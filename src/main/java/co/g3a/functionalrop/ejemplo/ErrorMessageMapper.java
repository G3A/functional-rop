package co.g3a.functionalrop.ejemplo;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ErrorMessageMapper {

    public static String toUserMessage(AppError error) {
        return switch (error) {
            case AppError.NameBlank e -> "El nombre no debe estar en blanco.";
            case AppError.EmailBlank e -> "El email no debe estar en blanco.";
            case AppError.EmailInvalid e -> "El email '%s' es inválido.".formatted(e.email());
            case AppError.PasswordBlank e -> "La contraseña no debe estar vacía.";
            case AppError.PasswordTooShort e -> "La contraseña es demasiado corta.";
            case AppError.NameTooShort e -> "El nombre es demasiado corto.";
            case AppError.UnderAge e -> "Debe ser mayor de edad. Edad: " + e.age();
            case AppError.DbError e -> "Error de base de datos: " + e.detail();
            case AppError.EmailSendError e -> "Error al enviar email: " + e.detail();
            case AppError.ActivationCodeError e -> "Error generando código: " + e.detail();
            case AppError.MultipleErrors e -> 
                "Se encontraron varios errores:\n" + e.errors().stream()
                    .map(ErrorMessageMapper::toUserMessage)
                    .map(msg -> " - " + msg)
                    .collect(Collectors.joining("\n"));
        };
    }
}