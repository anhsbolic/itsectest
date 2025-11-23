package dev.harscode.itsectest.adapters.persistence.auditlog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "activity", nullable = false, length = 255)
    private String activity;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "ip_address_enc")
    private byte[] ipAddressEnc;

    @Column(name = "ip_address_hash")
    private String ipAddressHash;

    @Column(name = "user_agent_enc")
    private byte[] userAgentEnc;

    @Column(name = "user_agent_hash")
    private String userAgentHash;

    @Column(name = "activity_time", nullable = false)
    private Instant activityTime;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.activityTime == null) this.activityTime = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
