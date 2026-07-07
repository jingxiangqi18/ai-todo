package com.qijx.aitodo.user.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.qijx.aitodo.user.entity.User;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-minutes}")
    private Long expirationMinutes;

    public String generateToken(User user){
        Instant now = Instant.now();
        Instant expireAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return JWT.create()
                .withSubject(String.valueOf(user.getId()))
                .withClaim("username", user.getUsername())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expireAt))
                .sign(Algorithm.HMAC256(secret));
    }

    private Long parseUserId(String token){
        try{
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return Long.valueOf(decodedJWT.getSubject());
        }catch(JWTVerificationException | NumberFormatException e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效或已过期的登录凭证");
        }
    }

    public Long parseUserIdFromAuthorizationHeader(String authorizationHeader){
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少登陆凭证");
        }

        String token = authorizationHeader.substring(7);

        return parseUserId(token);
    }
}
