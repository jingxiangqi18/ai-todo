package com.qijx.aitodo.group.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupTaskStatusUpdateRequest{
    @NotBlank(message = "状态不能为空")
    private String status;
}