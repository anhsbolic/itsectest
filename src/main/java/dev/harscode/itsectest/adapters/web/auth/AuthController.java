package dev.harscode.itsectest.adapters.web.auth;

import dev.harscode.itsectest.application.auth.RegisterUserCommand;
import dev.harscode.itsectest.application.auth.RegisterUserResult;
import dev.harscode.itsectest.application.auth.RegisterUserUsecase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUsecase registerUserUsecase;

    public AuthController(RegisterUserUsecase registerUserUsecase) {
        this.registerUserUsecase = registerUserUsecase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand cmd = new RegisterUserCommand(
                request.username(),
                request.email(),
                request.password(),
                request.name()
        );

        RegisterUserResult result = registerUserUsecase.register(cmd);

        RegisterResponse response = new RegisterResponse(
                result.userId(),
                result.username(),
                result.email(),
                result.role(),
                result.status()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
