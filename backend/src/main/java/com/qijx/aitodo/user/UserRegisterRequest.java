package com.qijx.aitodo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UserRegisterRequest {
    @NotBlank(message = "用户名不得为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3到20个字符之间")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式有误")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
    private String password;
}
