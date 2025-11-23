package dev.harscode.itsectest.ports.repository;

import dev.harscode.itsectest.domain.audit.AuditLog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    void save(AuditLog auditLog);

    List<AuditLog> findAll();

    Optional<AuditLog> findById(UUID id);
}
