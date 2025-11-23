package dev.harscode.itsectest.domain.auth;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class MfaOtpSession {
    private String id;
    private UUID userId;
    private String otpHash;
    private int attempts;
    private int maxAttempts;
    private int requests;
    private int maxRequests;
    private Instant createdAt;
    private Instant expiresAt;
    private String uaHash;
    private String ipHash;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean canAttemptMore() {
        return attempts < maxAttempts;
    }

    public boolean canRequestMore() {
        return requests < maxRequests;
    }
}
