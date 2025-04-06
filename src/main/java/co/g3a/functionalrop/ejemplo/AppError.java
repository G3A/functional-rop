package co.g3a.functionalrop.ejemplo;

import java.util.List;

public sealed interface AppError permits
        AppError.NameBlank,
        AppError.EmailBlank,
        AppError.EmailInvalid,
        AppError.PasswordBlank,
        AppError.PasswordTooShort,
        AppError.NameTooShort,
        AppError.UnderAge,
        AppError.DbError,
        AppError.EmailSendError,
        AppError.ActivationCodeError,
        AppError.MultipleErrors {

    record NameBlank() implements AppError {}
    record EmailBlank() implements AppError {}
    record EmailInvalid(String email) implements AppError {}
    record PasswordBlank() implements AppError {}
    record PasswordTooShort() implements AppError {}
    record NameTooShort() implements AppError {}
    record UnderAge(int age) implements AppError {}
    record DbError(String detail) implements AppError {}
    record EmailSendError(String detail) implements AppError {}
    record ActivationCodeError(String detail) implements AppError {}
    record MultipleErrors(List<AppError> errors) implements AppError {}
}