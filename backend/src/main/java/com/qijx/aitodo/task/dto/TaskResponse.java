package com.qijx.aitodo.task.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TaskResponse {
    private Long id;

    private String title;

    private String description;

    private String status;

    private String priority;

    private LocalDateTime dueAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
