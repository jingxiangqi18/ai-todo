package com.qijx.aitodo.task.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("task_steps")
public class TaskStep {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String title;

    private Boolean completed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
