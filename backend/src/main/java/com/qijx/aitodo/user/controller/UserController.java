package com.qijx.aitodo.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.user.dto.UserLoginRequest;
import com.qijx.aitodo.user.dto.UserRegisterRequest;
import com.qijx.aitodo.user.dto.UserResponse;
import com.qijx.aitodo.user.service.JwtService;
import com.qijx.aitodo.user.service.UserService;
import com.qijx.aitodo.user.dto.UserLoginResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService){
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegisterRequest request){
        return userService.register(request);
    }

    @PostMapping("/login")
    public UserLoginResponse login(@Valid @RequestBody UserLoginRequest request){
        return userService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authorizationHeader){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);
        
        return userService.getCurrentUser(userId);
    }
}

