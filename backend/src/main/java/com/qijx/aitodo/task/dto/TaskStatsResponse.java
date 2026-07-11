package com.qijx.aitodo.task.dto;

import lombok.Data;

@Data
public class TaskStatsResponse {
    private long total;

    private long todo;

    private long inProgress;

    private long done;

    private long highPriority;

    private long dueToday;

    private long overdue;
}
