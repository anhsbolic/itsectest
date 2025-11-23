package dev.harscode.itsectest.adapters.persistence.auditlog;

import dev.harscode.itsectest.domain.audit.AuditLog;
import dev.harscode.itsectest.ports.repository.AuditLogRepository;
import dev.harscode.itsectest.ports.crypt.PiiCrypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public List<AuditLog> findAll() {
        return jpa.findAllByDeletedAtIsNull()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        return jpa.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .map(this::toDomain);
    }

    private AuditLog toDomain(AuditLogEntity e) {
        AuditLog d = new AuditLog();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setActivity(e.getActivity());
        d.setEntityType(e.getEntityType());
        d.setEntityId(e.getEntityId());
        d.setSuccess(e.isSuccess());
        d.setIpAddress(piiCrypto.decrypt(e.getIpAddressEnc()));
        d.setIpAddressHash(e.getIpAddressHash());
        d.setUserAgent(piiCrypto.decrypt(e.getUserAgentEnc()));
        d.setUserAgentHash(e.getUserAgentHash());
        d.setActivityTime(e.getActivityTime());
        d.setDescription(e.getDescription());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setDeletedAt(e.getDeletedAt());
        return d;
    }
}
