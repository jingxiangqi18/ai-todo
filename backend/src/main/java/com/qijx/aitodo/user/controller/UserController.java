package com.qijx.aitodo.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.qijx.aitodo.user.dto.UserLoginRequest;
import com.qijx.aitodo.user.dto.UserRegisterRequest;
import com.qijx.aitodo.user.dto.UserResponse;
import com.qijx.aitodo.user.service.LoginRateLimitService;
import com.qijx.aitodo.user.service.UserService;
import com.qijx.aitodo.user.dto.UserLoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final LoginRateLimitService loginRateLimitService;

    public UserController(UserService userService, LoginRateLimitService loginRateLimitService){
        this.userService = userService;
        this.loginRateLimitService = loginRateLimitService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegisterRequest request){
        return userService.register(request);
    }

    @PostMapping("/login")
    public UserLoginResponse login(
        @Valid @RequestBody UserLoginRequest request,
        HttpServletRequest httpServletRequest
    ){
        String clientIp = httpServletRequest.getRemoteAddr();

        loginRateLimitService.checkLoginRateLimit(clientIp);
        
        return userService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Long userId){
        return userService.getCurrentUser(userId);
    }
}
