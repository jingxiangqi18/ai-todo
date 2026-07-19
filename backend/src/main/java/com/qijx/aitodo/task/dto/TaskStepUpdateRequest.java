package com.qijx.aitodo.task.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskStepUpdateRequest {
    @Size(max = 100, message = "步骤标题不能超过100个字符")
    private String title;

    private Boolean completed;
}
