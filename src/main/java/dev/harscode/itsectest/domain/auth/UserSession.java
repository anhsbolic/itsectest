package dev.harscode.itsectest.domain.auth;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserSession {
    private UUID id;
    private UUID userId;
    private UUID sessionId;
    private String refreshTokenHash;
    private String userAgent;
    private String ipAddress;
    private String userAgentHash;
    private String ipAddressHash;
    private String countryCode;
    private Instant expiresAt;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
