package dev.harscode.itsectest.adapters.persistence.user;

import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.ports.PiiCrypto;
import dev.harscode.itsectest.ports.UserSessionRepository;
import org.springframework.stereotype.Component;

@Component
public class UserSessionRepositoryJpaAdapter implements UserSessionRepository {

    private final UserSessionJpaRepository jpa;
    private final PiiCrypto encryptionService;

    public UserSessionRepositoryJpaAdapter(
            UserSessionJpaRepository jpa,
            PiiCrypto encryptionService
    ) {
        this.jpa = jpa;
        this.encryptionService = encryptionService;
    }

    @Override
    public UserSession create(UserSession session) {
        UserSessionEntity e = new UserSessionEntity();
        e.setUserId(session.getUserId());
        e.setSessionId(session.getSessionId());
        e.setRefreshTokenHash(session.getRefreshTokenHash());

        String uaPlain = session.getUserAgent() == null ? "" : session.getUserAgent();
        String ipPlain = session.getIpAddress() == null ? "" : session.getIpAddress();

        e.setUserAgentEnc(encryptionService.encrypt(uaPlain));
        e.setUserAgentHash(session.getUserAgentHash());

        e.setIpAddressEnc(encryptionService.encrypt(ipPlain));
        e.setIpAddressHash(session.getIpAddressHash());

        e.setCountryCode(session.getCountryCode());
        e.setExpiresAt(session.getExpiresAt());

        UserSessionEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    private UserSession toDomain(UserSessionEntity e) {
        UserSession s = new UserSession();
        s.setId(e.getId());
        s.setUserId(e.getUserId());
        s.setSessionId(e.getSessionId());
        s.setRefreshTokenHash(e.getRefreshTokenHash());
        s.setUserAgentHash(e.getUserAgentHash());
        s.setIpAddressHash(e.getIpAddressHash());
        s.setCountryCode(e.getCountryCode());
        s.setExpiresAt(e.getExpiresAt());
        s.setRevokedAt(e.getRevokedAt());
        s.setDeletedAt(e.getDeletedAt());
        s.setCreatedAt(e.getCreatedAt());
        s.setUpdatedAt(e.getUpdatedAt());

        // s.setUserAgent(encryptionService.decrypt(e.getUserAgentEnc()));
        // s.setIpAddress(encryptionService.decrypt(e.getIpAddressEnc()));

        return s;
    }
}
