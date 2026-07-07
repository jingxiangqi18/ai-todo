package com.qijx.aitodo.task.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tasks")
public class Task {
    @TableId(type =  IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    private String status;

    private String priority;

    private LocalDateTime dueAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
