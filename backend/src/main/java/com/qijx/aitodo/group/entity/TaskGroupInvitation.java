package com.qijx.aitodo.group.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("task_group_invitations")
public class TaskGroupInvitation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long inviterId;

    private Long inviteeId;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
