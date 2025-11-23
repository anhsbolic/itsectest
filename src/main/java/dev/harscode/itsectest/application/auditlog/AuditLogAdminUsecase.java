package dev.harscode.itsectest.application.auditlog;

import java.util.List;
import java.util.UUID;

public interface AuditLogAdminUsecase {

    List<AuditLogResult> list(AuditLogFilter filter);

    AuditLogResult getById(UUID id);
}
