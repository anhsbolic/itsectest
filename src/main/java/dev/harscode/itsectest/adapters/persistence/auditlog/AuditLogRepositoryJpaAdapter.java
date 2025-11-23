package dev.harscode.itsectest.adapters.persistence.auditlog;

import dev.harscode.itsectest.domain.audit.AuditLog;
import dev.harscode.itsectest.ports.AuditLogRepository;
import dev.harscode.itsectest.ports.PiiCrypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryJpaAdapter implements AuditLogRepository {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRepositoryJpaAdapter.class);

    private final AuditLogJpaRepository jpa;
    private final PiiCrypto piiCrypto;

    public AuditLogRepositoryJpaAdapter(AuditLogJpaRepository jpa, PiiCrypto piiCrypto) {
        this.jpa = jpa;
        this.piiCrypto = piiCrypto;
    }

    @Override
    public void save(AuditLog model) {
        try {
            AuditLogEntity e = new AuditLogEntity();
            e.setUserId(model.getUserId());
            e.setActivity(model.getActivity());
            e.setEntityType(model.getEntityType());
            e.setEntityId(model.getEntityId());
            e.setSuccess(model.isSuccess());
            e.setDescription(model.getDescription());
            e.setActivityTime(model.getActivityTime());

            String ua = model.getUserAgent() == null ? "" : model.getUserAgent();
            String ip = model.getIpAddress() == null ? "" : model.getIpAddress();

            e.setUserAgentEnc(piiCrypto.encrypt(ua));
            e.setUserAgentHash(model.getUserAgentHash());
            e.setIpAddressEnc(piiCrypto.encrypt(ip));
            e.setIpAddressHash(model.getIpAddressHash());

            jpa.save(e);
        } catch (Exception ex) {
            log.warn("Failed to save audit log: {}", ex.getMessage());
        }
    }
}
