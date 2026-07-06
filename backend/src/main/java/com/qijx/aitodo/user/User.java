package com.qijx.aitodo.user;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String passwordHash;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
