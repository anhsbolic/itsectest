package dev.harscode.itsectest.adapters.persistence.usersession;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_sessions")
public class UserSessionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Column(name = "user_agent_enc", nullable = false)
    private byte[] userAgentEnc;

    @Column(name = "user_agent_hash", nullable = false)
    private String userAgentHash;

    @Column(name = "ip_address_enc", nullable = false)
    private byte[] ipAddressEnc;

    @Column(name = "ip_address_hash", nullable = false)
    private String ipAddressHash;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.sessionId == null) {
            this.sessionId = UUID.randomUUID();
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
