package com.qijx.aitodo.ai.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiRateLimitService {
    private static final String KEY_PREFIX = "rate_limit:ai:";
    private static final long MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    public AiRateLimitService(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void checkRateLimit(Long userId){
        String key = KEY_PREFIX + userId;

        Long count = stringRedisTemplate
                .opsForValue()
                .increment(key);

        if(count == null){
            throw new IllegalStateException("AI限流计数失败");
        }

        if(count.longValue() == 1L){
            stringRedisTemplate.expire(key, WINDOW);
        }

        if(count.longValue() > MAX_REQUESTS){
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI请求过于频繁，请1分钟后再试");
        }
    }
}
