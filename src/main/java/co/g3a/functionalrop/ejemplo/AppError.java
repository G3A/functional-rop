package co.g3a.functionalrop.ejemplo;

public sealed interface AppError permits AppError.EmailInvalid, AppError.EmailBlank {
     record EmailInvalid(String email) implements AppError {}
     record EmailBlank() implements AppError {}
}

