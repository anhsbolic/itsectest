package dev.harscode.itsectest.application.auth.login;

import dev.harscode.itsectest.domain.audit.AuditLog;
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
    private final AuditLogRepository auditLogRepository;

    public LoginUserService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            TokenHashService tokenHashService,
            LoginAttemptService loginAttemptService,
            AuditLogRepository auditLogRepository
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.tokenHashService = tokenHashService;
        this.loginAttemptService = loginAttemptService;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public LoginUserResult login(LoginUserCommand cmd) {
        String usernameOrEmail = cmd.usernameOrEmail().trim().toLowerCase();
        String rawPassword = cmd.rawPassword();
        String userAgent = cmd.userAgent() == null ? "" : cmd.userAgent().trim();
        String uaHash = tokenHashService.hash(userAgent);
        String ipAddress = cmd.ipAddress() == null ? "" : cmd.ipAddress().trim();
        String ipHash = tokenHashService.hash(ipAddress);

        // log login attempt
        auditLogRepository.save(buildAuditLog(
                "LOGIN_ATTEMPT",
                "User attempted login",
                false,
                userAgent,
                ipAddress,
                uaHash,
                ipHash,
                null,
                "USER",
                null
        ));

        // login attempts key
        String attemptKey = tokenHashService.hash(usernameOrEmail);

        // make sure the user is not blocked
        loginAttemptService.assertNotBlocked(attemptKey);

        // Find the user by username or email
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        if (userOpt.isEmpty()) {
            loginAttemptService.recordFailure(attemptKey);
            auditLogRepository.save(buildAuditLog(
                    "LOGIN_FAILED",
                    "Invalid credentials",
                    false,
                    userAgent,
                    ipAddress,
                    uaHash,
                    ipHash,
                    null,
                    "USER",
                    null
            ));

            throw new UnauthorizedException("Invalid credentials");
        }
        User user = userOpt.get();
        if (!user.getStatus().equals("active") || !user.isEmailVerified()) {
            auditLogRepository.save(buildAuditLog(
                    "LOGIN_FAILED",
                    "Invalid credentials",
                    false,
                    userAgent,
                    ipAddress,
                    uaHash,
                    ipHash,
                    null,
                    "USER",
                    null
            ));
            throw new UnauthorizedException("User is not active or email not verified");
        }

        // Verify password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            auditLogRepository.save(buildAuditLog(
                    "LOGIN_FAILED",
                    "Invalid credentials",
                    false,
                    userAgent,
                    ipAddress,
                    uaHash,
                    ipHash,
                    user.getId(),
                    "USER",
                    user.getId()
            ));
            loginAttemptService.recordFailure(attemptKey);
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

        // log success login
        auditLogRepository.save(buildAuditLog(
                "LOGIN_SUCCESS",
                "Login successful",
                true,
                userAgent,
                ipAddress,
                uaHash,
                ipHash,
                user.getId(),
                "USER",
                user.getId()
        ));

        // Return
        return new LoginUserResult(user, profile, accessToken, refreshToken);
    }

    private AuditLog buildAuditLog(
            String activity,
            String description,
            boolean success,
            String userAgent,
            String ipAddress,
            String uaHash,
            String ipHash,
            UUID userId,
            String entityType,
            UUID entityId
    ) {
        AuditLog log = new AuditLog();
        log.setActivity(activity);
        log.setDescription(description);
        log.setSuccess(success);
        log.setUserAgent(userAgent);
        log.setUserAgentHash(uaHash);
        log.setIpAddress(ipAddress);
        log.setIpAddressHash(ipHash);
        log.setUserId(userId);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setActivityTime(Instant.now());
        return log;
    }
}