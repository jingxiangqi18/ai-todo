package com.qijx.aitodo.task.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskUpdateRequest {
    @Size(max = 100, message = "任务标题不能超过100个字符")
    private String title;

    @Size(max = 100, message = "任务描述不能超过100个字符")
    private String description;

    private String priority;

    private LocalDateTime dueAt;
}
