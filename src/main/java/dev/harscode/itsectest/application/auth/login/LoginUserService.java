package dev.harscode.itsectest.application.auth.login;

import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.*;
import dev.harscode.itsectest.security.jwt.JwtTokenService;
import dev.harscode.itsectest.security.token.TokenHashService;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginUserService implements LoginUserUsecase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenHashService tokenHashService;
    private final LoginAttemptService loginAttemptService;

    public LoginUserService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            TokenHashService tokenHashService,
            LoginAttemptService loginAttemptService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.tokenHashService = tokenHashService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @Transactional
    public LoginUserResult login(LoginUserCommand cmd) {
        String usernameOrEmail = cmd.usernameOrEmail().trim().toLowerCase();
        String rawPassword = cmd.rawPassword();
        String userAgent = cmd.userAgent() == null ? "" : cmd.userAgent().trim();
        String ipAddress = cmd.ipAddress() == null ? "" : cmd.ipAddress().trim();

        // login attempts key
        String attemptKey = tokenHashService.hash(usernameOrEmail);

        // make sure the user is not blocked
        loginAttemptService.assertNotBlocked(attemptKey);

        // Find the user by username or email
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("Invalid credentials");
        }
        User user = userOpt.get();
        if (!user.getStatus().equals("active") || !user.isEmailVerified()) {
            throw new UnauthorizedException("User is not active or email not verified");
        }

        // Verify password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Get Profile
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        // Generate a refresh token
        String refreshToken = tokenHashService.generateRefreshToken();

        // Create Session
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setSessionId(UUID.randomUUID());
        session.setRefreshTokenHash(refreshToken);
        session.setUserAgent(userAgent);
        session.setUserAgentHash(tokenHashService.hash(userAgent));
        session.setIpAddress(ipAddress);
        session.setIpAddressHash(tokenHashService.hash(ipAddress));
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        UserSession createdSession = userSessionRepository.create(session);

        // Generate an access token
        String accessToken = jwtTokenService.generateAccessToken(
                user.getId().toString(),
                createdSession.getId().toString(),
                user.getRole(),
                false
        );

        // Reset login attempts
        loginAttemptService.reset(attemptKey);

        // Return
        return new LoginUserResult(user, profile, accessToken, refreshToken);
    }
}