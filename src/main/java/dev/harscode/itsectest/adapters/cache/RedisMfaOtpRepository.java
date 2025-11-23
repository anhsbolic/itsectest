package dev.harscode.itsectest.adapters.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harscode.itsectest.domain.auth.MfaOtpSession;
import dev.harscode.itsectest.ports.repository.MfaOtpRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisMfaOtpRepository implements MfaOtpRepository {

    private static final String KEY_PREFIX = "auth:mfa:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisMfaOtpRepository(StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public MfaOtpSession create(MfaOtpSession session) {
        if (session.getId() == null) {
            session.setId(UUID.randomUUID().toString());
        }
        save(session);
        return session;
    }

    @Override
    public Optional<MfaOtpSession> findById(String id) {
        String key = KEY_PREFIX + id;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            MfaOtpSession s = objectMapper.readValue(json, MfaOtpSession.class);
            return Optional.of(s);
        } catch (JsonProcessingException e) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void save(MfaOtpSession session) {
        String key = KEY_PREFIX + session.getId();
        try {
            String json = objectMapper.writeValueAsString(session);

            Duration ttl = Duration.between(
                    java.time.Instant.now(),
                    session.getExpiresAt()
            );
            if (ttl.isNegative()) {
                ttl = Duration.ofSeconds(0);
            }
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize MFA session", e);
        }
    }

    @Override
    public void delete(String id) {
        redisTemplate.delete(KEY_PREFIX + id);
    }
}
