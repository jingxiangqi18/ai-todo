package com.qijx.aitodo.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskStepDraftRequest {
    @Size(max = 500, message = "任务拆解要求不得超过500个字符")
    private String instruction;
}
