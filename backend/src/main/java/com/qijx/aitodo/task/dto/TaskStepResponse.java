package com.qijx.aitodo.task.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TaskStepResponse {
    private Long id;

    private String title;

    private Boolean completed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
