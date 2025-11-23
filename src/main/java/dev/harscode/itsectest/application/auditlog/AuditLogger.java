package dev.harscode.itsectest.application.auditlog;

import dev.harscode.itsectest.domain.audit.AuditLog;
import dev.harscode.itsectest.ports.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditLogger {

    private final AuditLogRepository repo;

    public AuditLogger(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(UUID userId,
                    String activity,
                    String entityType,
                    UUID entityId,
                    boolean success,
                    String ipEnc,
                    String ipHash,
                    String uaEnc,
                    String uaHash,
                    String description) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setActivity(activity);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setSuccess(success);
        log.setIpAddress(ipEnc);
        log.setIpAddressHash(ipHash);
        log.setUserAgent(uaEnc);
        log.setUserAgentHash(uaHash);
        log.setActivityTime(Instant.now());
        log.setDescription(description);
        repo.save(log);
    }
}
