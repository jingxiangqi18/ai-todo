package com.qijx.aitodo.common.redis;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisAtomicRateLimiter {
    private static final String RATE_LIMIT_SCRIPT = """
            local current = redis.call('GET', KEYS[1])

            if current and tonumber(current) >= tonumber(ARGV[1]) then
                return 0
            end

            local newCount = redis.call('INCR', KEYS[1])
            local ttl = redis.call('PTTL', KEYS[1])

            if newCount == 1 or ttl < 0 then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end

            return 1
            """;

    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisAtomicRateLimiter(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean isAllowed(String key, long maxRequests, Duration window){
        Long result = stringRedisTemplate.execute(
            SCRIPT,
            List.of(key),
            Long.toString(maxRequests),
            Long.toString(window.toMillis())
        );

        if(result == null){
            throw new IllegalStateException("Redis限流脚本没有返回结果");
        }

        return result.longValue() == 1L;
    }
}
