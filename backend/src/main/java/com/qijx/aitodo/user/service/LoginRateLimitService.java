package com.qijx.aitodo.user.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginRateLimitService {
    private static final long MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    public LoginRateLimitService(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void checkLoginRateLimit(String clientIp){
        String key = "rate_limit:login:" + clientIp;

        Long count = stringRedisTemplate.opsForValue().increment(key);

        if(count != null && count == 1){
            stringRedisTemplate.expire(key, WINDOW);
        }

        if(count != null && count > MAX_ATTEMPTS){
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请1分钟后再试");
        }
    }
}
