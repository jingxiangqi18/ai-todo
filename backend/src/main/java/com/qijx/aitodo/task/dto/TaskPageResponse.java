package com.qijx.aitodo.task.dto;

import java.util.List;

import lombok.Data;

@Data
public class TaskPageResponse {
    private List<TaskResponse> records;

    private long page;

    private long size;

    private long total;

    private long pages;
}
