package dev.harscode.itsectest.adapters.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionJpaRepository extends JpaRepository<UserSessionEntity, UUID> {
    @Query("""
            SELECT s FROM UserSessionEntity s
            WHERE s.refreshTokenHash = :refreshTokenHash
              AND s.revokedAt IS NULL
              AND s.deletedAt IS NULL
              AND s.expiresAt > :now
            """)
    Optional<UserSessionEntity> findValidByRefreshTokenHash(String refreshTokenHash, Instant now);
}
