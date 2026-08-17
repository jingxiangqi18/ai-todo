package com.qijx.aitodo.group.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupTaskUpdateRequest {
    @Size(max = 100, message = "标题不得超过100个字符")
    private String title;

    @Size(max = 1000, message = "描述不得超过1000个字符")
    private String description;

    private String priority;

    private LocalDateTime dueAt;
}
