package dev.harscode.itsectest.adapters.persistence.user;

import dev.harscode.itsectest.domain.auth.UserToken;
import dev.harscode.itsectest.ports.UserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserTokenRepositoryJpaAdapter implements UserTokenRepository {

    private final UserTokenJpaRepository jpa;

    public UserTokenRepositoryJpaAdapter(UserTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public UserToken create(UserToken token) {
        UserTokenEntity e = new UserTokenEntity();
        e.setUserId(token.getUserId());
        e.setTokenHash(token.getTokenHash());
        e.setTokenType(token.getTokenType());
        e.setExpiresAt(token.getExpiresAt());
        UserTokenEntity saved = jpa.save(e);

        token.setId(saved.getId());
        return token;
    }

    @Override
    public Optional<UserToken> findValidToken(String tokenHash, String tokenType) {
        Logger log = LoggerFactory.getLogger(getClass());
        log.info("Finding valid token {} of type {} at {}", tokenHash, tokenType, Instant.now());

        return jpa.findValidToken(tokenHash, tokenType, Instant.now()).map(this::toDomain);
    }

    @Override
    public void markUsed(UUID tokenId) {
        jpa.findById(tokenId).ifPresent(e -> {
            e.setUsedAt(Instant.now());
            jpa.save(e);
        });
    }

    private UserToken toDomain(UserTokenEntity e) {
        UserToken t = new UserToken();
        t.setId(e.getId());
        t.setUserId(e.getUserId());
        t.setTokenHash(e.getTokenHash());
        t.setTokenType(e.getTokenType());
        t.setExpiresAt(e.getExpiresAt());
        t.setUsedAt(e.getUsedAt());
        t.setRevokedAt(e.getRevokedAt());
        t.setCreatedAt(e.getCreatedAt());
        t.setUpdatedAt(e.getUpdatedAt());
        return t;
    }
}
