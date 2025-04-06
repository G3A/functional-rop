package co.g3a.functionalrop.ejemplo;

import co.g3a.functionalrop.ejemplo.errorhandling.builder.ApiErrorResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletionStage;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    private final UseCase useCase;
    private final ApiErrorResponseBuilder errorBuilder;

    public UsuarioController(UseCase useCase, ApiErrorResponseBuilder errorBuilder) {
        this.useCase = useCase;
        this.errorBuilder = errorBuilder;
    }

    @PutMapping
    public CompletionStage<ResponseEntity<?>> actualizar(
            @RequestBody UseCase.Request request,
            HttpServletRequest httpRequest
    ) {
        return useCase.executeUseCase(request)
                .thenApply(result -> result.fold(
                        error -> errorBuilder.from(error, httpRequest),
                        ResponseEntity::ok
                ));
    }
}