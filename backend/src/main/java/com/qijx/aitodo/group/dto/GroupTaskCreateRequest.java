package com.qijx.aitodo.group.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupTaskCreateRequest {
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题不能超过100个字符")
    private String title;

    @Size(max = 1000, message = "任务描述不能超过1000个字符")
    private String description;

    private Long assigneeId;

    private String priority;

    private LocalDateTime dueAt;
}
