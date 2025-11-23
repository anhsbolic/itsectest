package dev.harscode.itsectest.adapters.web.auth;

import dev.harscode.itsectest.application.auth.*;
import dev.harscode.itsectest.domain.user.AuthUser;
import dev.harscode.itsectest.web.dto.ApiResponse;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUsecase registerUserUsecase;
    private final EmailVerificationUsecase emailVerificationUsecase;
    private final LoginUserUsecase loginUserUsecase;
    private final RefreshTokenUsecase refreshTokenUsecase;
    private final GetCurrentUserUsecase getCurrentUserUsecase;
    private final LogoutUsecase logoutUsecase;

    public AuthController(
            RegisterUserUsecase registerUserUsecase,
            EmailVerificationUsecase emailVerificationUsecase,
            LoginUserUsecase loginUserUsecase,
            RefreshTokenUsecase refreshTokenUsecase,
            GetCurrentUserUsecase getCurrentUserUsecase,
            LogoutUsecase logoutUsecase
    ) {
        this.registerUserUsecase = registerUserUsecase;
        this.emailVerificationUsecase = emailVerificationUsecase;
        this.loginUserUsecase = loginUserUsecase;
        this.refreshTokenUsecase = refreshTokenUsecase;
        this.getCurrentUserUsecase = getCurrentUserUsecase;
        this.logoutUsecase = logoutUsecase;
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

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token cookie is missing");
        }

        String ua = (String) request.getAttribute("fingerprint.ua");
        String ip = (String) request.getAttribute("fingerprint.ip");

        RefreshTokenCommand cmd = new RefreshTokenCommand(refreshToken, ua, ip);
        RefreshTokenResult result = refreshTokenUsecase.refresh(cmd);

        // mapping user ke AuthUser
        AuthUser authUser = new AuthUser();
        authUser.setId(result.user().getId());
        authUser.setUsername(result.user().getUsername());
        authUser.setEmail(result.user().getEmail());
        authUser.setFullName(result.profile() != null ? result.profile().getFullName() : null);
        authUser.setRole(result.user().getRole());

        RefreshTokenResponse body = new RefreshTokenResponse(result.accessToken(), authUser);

        boolean isSecure = false; // TODO: set true for staging/production (HTTPS)
        String sameSite = "Lax"; // TODO : set "Strict" for staging/production (HTTPS)
        ResponseCookie cookie = ResponseCookie.from("refresh_token", result.newRefreshTokenRaw())
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(7 * 24 * 60 * 60)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok("Token refreshed", body));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(HttpServletRequest request) {
        String userId = (String) request.getAttribute("auth.userId");
        if (userId == null) {
            throw new UnauthorizedException("Missing authentication");
        }

        GetCurrentUserResult result = getCurrentUserUsecase.getCurrentUser(UUID.fromString(userId));

        AuthUser authUser = new AuthUser();
        authUser.setId(result.user().getId());
        authUser.setUsername(result.user().getUsername());
        authUser.setEmail(result.user().getEmail());
        authUser.setFullName(result.profile() != null ? result.profile().getFullName() : null);
        authUser.setRole(result.user().getRole());

        MeResponse body = new MeResponse(authUser);

        return ResponseEntity.ok(ApiResponse.ok("Current user", body));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String userId = (String) request.getAttribute("auth.userId");
        String sessionId = (String) request.getAttribute("auth.sessionId");

        if (userId == null || sessionId == null) {
            throw new UnauthorizedException("Missing authentication");
        }

        // Revoke session
        logoutUsecase.logout(UUID.fromString(userId), UUID.fromString(sessionId));

        // Clear refresh_token cookie
        boolean isSecure = false; // TODO: set true for staging/production (HTTPS)
        String sameSite = "Lax"; // TODO : set "Strict" for staging/production (HTTPS)
        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(ApiResponse.ok("Logged out", null));
    }
}
