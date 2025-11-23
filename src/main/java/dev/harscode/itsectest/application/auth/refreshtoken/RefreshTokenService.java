package dev.harscode.itsectest.application.auth.refreshtoken;

import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.ports.repository.UserSessionRepository;
import dev.harscode.itsectest.security.jwt.JwtTokenService;
import dev.harscode.itsectest.security.token.TokenHashService;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RefreshTokenService implements RefreshTokenUsecase {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TokenHashService tokenHashService;
    private final JwtTokenService jwtTokenService;

    public RefreshTokenService(
            UserSessionRepository userSessionRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            TokenHashService tokenHashService,
            JwtTokenService jwtTokenService
    ) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.tokenHashService = tokenHashService;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    @Transactional
    public RefreshTokenResult refresh(RefreshTokenCommand cmd) {
        if (cmd.rawRefreshToken() == null || cmd.rawRefreshToken().isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }

        String raw = cmd.rawRefreshToken().trim();
        String ua = cmd.userAgent() == null ? "" : cmd.userAgent().trim();
        String ip = cmd.ipAddress() == null ? "" : cmd.ipAddress().trim();

        // Find Session by refresh token
        UserSession session = userSessionRepository
                .findValidByRefreshTokenHash(raw, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));

        // Validate user agent and ip address
        String uaHash = tokenHashService.hash(ua);
        String ipHash = tokenHashService.hash(ip);
        if (!uaHash.equals(session.getUserAgentHash()) || !ipHash.equals(session.getIpAddressHash())) {
            userSessionRepository.revokeSession(session.getSessionId(), Instant.now());
            throw new UnauthorizedException("Session is bound to another device or location");
        }

        // Find User
        User user = userRepository.findById(session.getUserId()).orElseThrow(() ->
                new UnauthorizedException("User not found")
        );

        if (!user.getStatus().equals("active") || !user.isEmailVerified()) {
            throw new UnauthorizedException("User is not active or email not verified");
        }

        // Find Profile
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        // Rotate refresh token
        String newRefreshRaw = tokenHashService.generateRefreshToken();
        String newRefreshHash = tokenHashService.hash(newRefreshRaw);
        session.setRefreshTokenHash(newRefreshHash);
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        userSessionRepository.save(session);

        // Generate a new access token
        String accessToken = jwtTokenService.generateAccessToken(
                user.getId().toString(),
                session.getSessionId().toString(),
                user.getRole(),
                false
        );

        return new RefreshTokenResult(user, profile, accessToken, newRefreshRaw);
    }
}
