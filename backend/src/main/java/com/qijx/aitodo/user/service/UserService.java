package com.qijx.aitodo.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.user.dto.UserLoginRequest;
import com.qijx.aitodo.user.dto.UserLoginResponse;
import com.qijx.aitodo.user.dto.UserRegisterRequest;
import com.qijx.aitodo.user.dto.UserResponse;
import com.qijx.aitodo.user.entity.User;
import com.qijx.aitodo.user.mapper.UserMapper;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, JwtService jwtService){
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    public UserResponse register(UserRegisterRequest request){
        Long usernameCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
        );

        if(usernameCount > 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }

        Long emailCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())
        );

        if(emailCount > 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邮箱已被使用");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userMapper.insert(user);

        return toResponse(user);
    }

    public UserLoginResponse login(UserLoginRequest request){
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
            .eq(User::getUsername, request.getAccount())
            .or()
            .eq(User::getEmail, request.getAccount())
        );

        if(user == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号或密码错误");
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if(!passwordMatches){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号或密码错误");
        }

        if(!"ACTIVE".equals(user.getStatus())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }

        UserLoginResponse response = new UserLoginResponse();
        response.setToken(jwtService.generateToken(user));
        response.setUser(toResponse(user));

        return response;
    }

    public UserResponse getCurrentUser(String authorizationHeader){
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少登录凭证");
        }

        String token = authorizationHeader.substring(7);
        Long userId = jwtService.parseUserId(token);

        User user = userMapper.selectById(userId);

        if(user == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        if(!"ACTIVE".equals(user.getStatus())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被封禁");
        }

        return toResponse(user);
    }

    private UserResponse toResponse(User user){
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }
}