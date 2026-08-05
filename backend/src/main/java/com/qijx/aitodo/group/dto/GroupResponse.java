package com.qijx.aitodo.group.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class GroupResponse {
    private Long id;

    private String name;

    private String description;

    private Long ownerId;

    private String currentUserRole;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
