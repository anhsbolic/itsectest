package dev.harscode.itsectest.domain.auth;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserToken {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private String tokenType;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
