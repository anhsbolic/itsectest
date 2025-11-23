package dev.harscode.itsectest.domain.audit;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AuditLog {
    private UUID id;
    private UUID userId;
    private String activity;
    private String entityType;
    private UUID entityId;
    private boolean success;
    private String ipAddress;
    private String ipAddressHash;
    private String userAgent;
    private String userAgentHash;
    private Instant activityTime;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
