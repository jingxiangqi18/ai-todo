package com.qijx.aitodo.group.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("group_tasks")
public class GroupTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long creatorId;

    private Long assigneeId;

    private String title;

    private String description;

    private String status;

    private String priority;

    private LocalDateTime dueAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
