package com.qijx.aitodo.user.service;

import java.time.Duration;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.aitodo.common.redis.RedisAtomicRateLimiter;

@Service
public class LoginRateLimitService {
    private static final long MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "rate_limit:login:";

    private final RedisAtomicRateLimiter redisAtomicRateLimiter;

    public LoginRateLimitService(RedisAtomicRateLimiter redisAtomicRateLimiter){
        this.redisAtomicRateLimiter = redisAtomicRateLimiter;
    }

    public void checkLoginRateLimit(String clientIp){
        String key = KEY_PREFIX + clientIp;

        try{
            boolean allowed = redisAtomicRateLimiter.isAllowed(key, MAX_ATTEMPTS, WINDOW);

            if(!allowed){
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请1分钟后再试");
            }
        }catch(DataAccessException | IllegalStateException exception){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "登录限流服务暂时不可用", exception);
        }
    }
}
