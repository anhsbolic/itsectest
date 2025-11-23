package dev.harscode.itsectest.security.otp;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class OtpRateLimiter {

    private static final String KEY_PREFIX = "auth:otp-req:";

    private final StringRedisTemplate redisTemplate;

    public OtpRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long incrementUserOtpRequests(UUID userId, Duration window) {
        String key = KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count == null ? 0L : count;
    }
}
