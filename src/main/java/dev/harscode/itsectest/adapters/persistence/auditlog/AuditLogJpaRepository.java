package dev.harscode.itsectest.adapters.persistence.auditlog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findAllByDeletedAtIsNull();
}
