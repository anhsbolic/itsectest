package dev.harscode.itsectest.adapters.persistence.usertoken;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserTokenJpaRepository extends JpaRepository<UserTokenEntity, UUID> {

    @Query("""
            SELECT t FROM UserTokenEntity t
            WHERE t.tokenHash = :tokenHash
              AND t.tokenType = :tokenType
              AND t.usedAt IS NULL
              AND t.revokedAt IS NULL
              AND t.deletedAt IS NULL
              AND t.expiresAt > :now
            """)
    Optional<UserTokenEntity> findValidToken(
            @Param("tokenHash") String tokenHash,
            @Param("tokenType") String tokenType,
            @Param("now") Instant now);
}

