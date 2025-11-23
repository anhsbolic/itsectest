package dev.harscode.itsectest.application.auditlog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResult(
        UUID id,
        UUID userId,
        String activity,
        String entityType,
        UUID entityId,
        boolean success,
        String ipAddressHash,
        String userAgentHash,
        Instant activityTime,
        String description
) {
}