package com.qijx.aitodo.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper){
        this.userMapper = userMapper;
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