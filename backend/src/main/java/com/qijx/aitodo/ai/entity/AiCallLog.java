package com.qijx.aitodo.ai.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("ai_call_logs")
public class AiCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String feature;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Long durationMs;

    private Boolean success;

    private String errorMessage;

    private LocalDateTime createdAt;
}
