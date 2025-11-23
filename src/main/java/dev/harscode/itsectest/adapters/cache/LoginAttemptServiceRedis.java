package dev.harscode.itsectest.adapters.cache;

import dev.harscode.itsectest.config.LoginAttemptProperties;
import dev.harscode.itsectest.ports.LoginAttemptService;
import dev.harscode.itsectest.web.exception.LoginLockedException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class LoginAttemptServiceRedis implements LoginAttemptService {

    private static final String FAIL_PREFIX = "auth:login:fail:";
    private static final String BLOCK_PREFIX = "auth:login:block:";

    private final StringRedisTemplate redis;
    private final LoginAttemptProperties props;

    public LoginAttemptServiceRedis(StringRedisTemplate redis, LoginAttemptProperties props) {
        this.redis = redis;
        this.props = props;
    }

    @Override
    public void assertNotBlocked(String key) {
        String blockKey = BLOCK_PREFIX + key;
        if (redis.hasKey(blockKey)) {
            Long ttl = redis.getExpire(blockKey, TimeUnit.SECONDS);
            long remaining = ttl > 0 ? ttl : props.getBlockSeconds();

            String message = "Too many failed login attempts. Please try again later.";
            throw new LoginLockedException(message, remaining);
        }
    }

    @Override
    public void recordFailure(String key) {
        String failKey = FAIL_PREFIX + key;

        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, props.getWindowSeconds(), TimeUnit.SECONDS);
        }

        if (count != null && count >= props.getMaxAttempts()) {
            redis.delete(failKey);
            String blockKey = BLOCK_PREFIX + key;
            redis.opsForValue().set(blockKey, "1", Duration.ofSeconds(props.getBlockSeconds()));
        }
    }

    @Override
    public void reset(String key) {
        redis.delete(FAIL_PREFIX + key);
        redis.delete(BLOCK_PREFIX + key);
    }
}
