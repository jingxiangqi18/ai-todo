package com.qijx.aitodo.group.dto;

import java.util.List;

import lombok.Data;

@Data
public class GroupTaskPageResponse {
    private List<GroupTaskResponse> records;

    private long page;

    private long size;

    private long total;

    private long pages;
}
