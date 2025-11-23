package dev.harscode.itsectest.adapters.web.auth;

import dev.harscode.itsectest.application.auth.*;
import dev.harscode.itsectest.domain.user.AuthUser;
import dev.harscode.itsectest.web.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUsecase registerUserUsecase;
    private final EmailVerificationUsecase emailVerificationUsecase;
    private final LoginUserUsecase loginUserUsecase;

    public AuthController(
            RegisterUserUsecase registerUserUsecase,
            EmailVerificationUsecase emailVerificationUsecase,
            LoginUserUsecase loginUserUsecase
    ) {
        this.registerUserUsecase = registerUserUsecase;
        this.emailVerificationUsecase = emailVerificationUsecase;
        this.loginUserUsecase = loginUserUsecase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest body) {
        RegisterUserCommand cmd = new RegisterUserCommand(
                body.username(),
                body.email(),
                body.password(),
                body.name()
        );

        RegisterUserResult result = registerUserUsecase.register(cmd);

        RegisterResponse response = new RegisterResponse(
                result.userId(),
                result.username(),
                result.email(),
                result.role(),
                result.status()
        );

        ApiResponse<RegisterResponse> envelope = ApiResponse.ok(
                "User registered successfully. Please verify your email.",
                response
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(envelope);
    }

    @GetMapping("/email-verification")
    public ResponseEntity<?> verifyEmailFromLink(@RequestParam("token") String token) {
        emailVerificationUsecase.verify(token);
        return ResponseEntity.ok(Map.of("message", "Email verified"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request
    ) {
        String userAgent = (String) request.getAttribute("fingerprint.ua");
        String ip = (String) request.getAttribute("fingerprint.ip");

        LoginUserCommand cmd = new LoginUserCommand(
                body.usernameOrEmail(),
                body.password(),
                userAgent,
                ip
        );

        LoginUserResult result = loginUserUsecase.login(cmd);

        AuthUser authUser = new AuthUser();
        authUser.setId(result.user().getId());
        authUser.setUsername(result.user().getUsername());
        authUser.setEmail(result.user().getEmail());
        authUser.setRole(result.user().getRole());
        if (result.profile() != null) {
            authUser.setFullName(result.profile().getFullName());
        }

        LoginResponse resBody = new LoginResponse(result.accessToken(), authUser);
        ApiResponse<LoginResponse> envelope = ApiResponse.ok("Login successful", resBody);

        boolean isSecure = false; // TODO: set true for staging/production (HTTPS)
        String sameSite = "Lax"; // TODO : set "Strict" for staging/production (HTTPS)
        ResponseCookie cookie = ResponseCookie.from("refresh_token", result.refreshTokenRaw())
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(7 * 24 * 60 * 60)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(envelope);
    }
}
