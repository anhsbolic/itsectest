package dev.harscode.itsectest.application.auditlog;

import java.util.UUID;

public record AuditLogFilter(
        UUID userId,
        String activity,
        Boolean success
) {
}
