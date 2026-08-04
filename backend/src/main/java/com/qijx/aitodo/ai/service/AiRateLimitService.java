package com.qijx.aitodo.ai.service;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessException;

import com.qijx.aitodo.common.redis.RedisAtomicRateLimiter;

@Service
public class AiRateLimitService {
    private static final String KEY_PREFIX = "rate_limit:ai:";
    private static final long MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RedisAtomicRateLimiter redisAtomicRateLimiter;

    public AiRateLimitService(RedisAtomicRateLimiter redisAtomicRateLimiter){
        this.redisAtomicRateLimiter = redisAtomicRateLimiter;
    }

    public void checkRateLimit(Long userId){
        String key = KEY_PREFIX + userId;

        try{
            boolean allowed = redisAtomicRateLimiter.isAllowed(key, MAX_REQUESTS, WINDOW);

            if(!allowed){
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI请求过于频繁，请1分钟后再试");
            }
        }catch(DataAccessException | IllegalStateException exception){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI限流服务暂时不可用", exception);
        }
    }
}
