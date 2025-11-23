package dev.harscode.itsectest.ports;

import dev.harscode.itsectest.domain.audit.AuditLog;

public interface AuditLogRepository {
    void save(AuditLog auditLog);
}
