package com.qijx.aitodo.user.dto;

import lombok.Data;

@Data
public class UserLoginResponse {
    private String token;

    private UserResponse user;
}
