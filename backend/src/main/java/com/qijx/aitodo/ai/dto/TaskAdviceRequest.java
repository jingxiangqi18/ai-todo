package com.qijx.aitodo.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskAdviceRequest {
    @NotBlank(message = "咨询内容不能为空")
    @Size(max = 1000, message = "咨询内容不能超过1000个字符")
    private String message;
}
