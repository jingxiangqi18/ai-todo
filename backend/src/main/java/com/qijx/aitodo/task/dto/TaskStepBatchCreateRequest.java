package com.qijx.aitodo.task.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskStepBatchCreateRequest {
    @NotEmpty(message = "至少需要选择一个步骤")
    @Size(max = 10, message = "一次最多保存10个步骤")
    private List<
            @NotBlank(message = "步骤标题不能为空")
            @Size(max = 100, message = "步骤标题不能超过100个字符") String
            > titles;
}
