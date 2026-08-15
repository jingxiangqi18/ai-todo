package com.qijx.aitodo.group.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class GroupTaskResponse {
    private Long id;

    private Long groupId;

    private Long creatorId;

    private String creatorName;

    private Long assigneeId;

    private String assigneeName;

    private String title;

    private String description;

    private String status;

    private String priority;

    private LocalDateTime dueAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
