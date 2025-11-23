package dev.harscode.itsectest.adapters.persistence.usersession;

import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.ports.PiiCrypto;
import dev.harscode.itsectest.ports.UserSessionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<UserSession> findValidByRefreshTokenHash(String refreshTokenHash, Instant now) {
        return jpa.findValidByRefreshTokenHash(refreshTokenHash, now).map(this::toDomain);
    }

    @Override
    public void save(UserSession session) {
        UserSessionEntity e = toEntity(session);
        jpa.save(e);
    }

    @Override
    public void revokeSession(UUID sessionId, Instant revokedAt) {
        jpa.findAll().stream()
                .filter(e -> e.getSessionId().equals(sessionId))
                .findFirst()
                .ifPresent(e -> {
                    e.setRevokedAt(revokedAt);
                    jpa.save(e);
                });
    }

    private UserSessionEntity toEntity(UserSession s) {
        UserSessionEntity e = new UserSessionEntity();
        e.setId(s.getId());
        e.setUserId(s.getUserId());
        e.setSessionId(s.getSessionId());
        e.setRefreshTokenHash(s.getRefreshTokenHash());
        e.setUserAgentEnc(encryptionService.encrypt(s.getUserAgent() == null ? "" : s.getUserAgent()));
        e.setUserAgentHash(s.getUserAgentHash());
        e.setIpAddressEnc(encryptionService.encrypt(s.getIpAddress() == null ? "" : s.getIpAddress()));
        e.setIpAddressHash(s.getIpAddressHash());
        e.setCountryCode(s.getCountryCode());
        e.setExpiresAt(s.getExpiresAt());
        e.setRevokedAt(s.getRevokedAt());
        e.setDeletedAt(s.getDeletedAt());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
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
