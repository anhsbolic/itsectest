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

    public void loginOtpSent(UUID userId, String ipEnc, String ipHash, String uaEnc, String uaHash) {
        log(userId, "LOGIN_MFA_OTP_SENT", null, null, true, ipEnc, ipHash, uaEnc, uaHash, null);
    }

    public void loginOtpResend(UUID userId, String ipEnc, String ipHash, String uaEnc, String uaHash) {
        log(userId, "LOGIN_MFA_OTP_RESEND", null, null, true, ipEnc, ipHash, uaEnc, uaHash, null);
    }

    public void loginOtpFailed(UUID userId, String ipEnc, String ipHash, String uaEnc, String uaHash, String reason) {
        log(userId, "LOGIN_MFA_OTP_FAILED", null, null, false, ipEnc, ipHash, uaEnc, uaHash, reason);
    }

    public void loginOtpSuccess(UUID userId, String ipEnc, String ipHash, String uaEnc, String uaHash) {
        log(userId, "LOGIN_MFA_OTP_SUCCESS", null, null, true, ipEnc, ipHash, uaEnc, uaHash, null);
    }

    public void loginAttempt(UUID userId, boolean success, String ipEnc, String ipHash, String uaEnc, String uaHash, String desc) {
        log(userId, "LOGIN_ATTEMPT", null, null, success, ipEnc, ipHash, uaEnc, uaHash, desc);
    }
}
