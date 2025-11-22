package dev.harscode.itsectest.adapters.web.auth;

import dev.harscode.itsectest.application.auth.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUsecase registerUserUsecase;
    private final EmailVerificationUsecase emailVerificationUsecase;

    public AuthController(
            RegisterUserUsecase registerUserUsecase,
            EmailVerificationUsecase emailVerificationUsecase
    ) {
        this.registerUserUsecase = registerUserUsecase;
        this.emailVerificationUsecase = emailVerificationUsecase;
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

    @GetMapping("/email-verification")
    public ResponseEntity<?> verifyEmailFromLink(@RequestParam("token") String token) {
        emailVerificationUsecase.verify(token);
        return ResponseEntity.ok(Map.of("message", "Email verified"));
    }
}
