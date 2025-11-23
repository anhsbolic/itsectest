package dev.harscode.itsectest.ports.repository;

import dev.harscode.itsectest.domain.auth.UserSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository {
    UserSession create(UserSession session);

    Optional<UserSession> findValidByRefreshTokenHash(String refreshTokenHash, Instant now);

    void save(UserSession session);

    void revokeSession(UUID sessionId, Instant revokedAt);
}
