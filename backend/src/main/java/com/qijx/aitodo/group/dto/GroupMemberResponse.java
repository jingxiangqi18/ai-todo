package com.qijx.aitodo.group.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class GroupMemberResponse {
    private Long userId;

    private String username;

    private String role;

    private LocalDateTime joinedAt;
}
