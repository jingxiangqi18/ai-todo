package com.qijx.aitodo.group.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.group.dto.GroupTaskCreateRequest;
import com.qijx.aitodo.group.dto.GroupTaskResponse;
import com.qijx.aitodo.group.dto.GroupTaskPageResponse;
import com.qijx.aitodo.group.service.GroupTaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupTaskController {
    private final GroupTaskService groupTaskService;

    public GroupTaskController(GroupTaskService groupTaskService){
        this.groupTaskService = groupTaskService;
    }

    @PostMapping("/{groupId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupTaskResponse createGroupTask(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @Valid @RequestBody GroupTaskCreateRequest request
    ){
        return groupTaskService.createGroupTask(userId, groupId, request);
    }

    @GetMapping("/{groupId}/tasks")
    public GroupTaskPageResponse listGroupTask(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @RequestParam(defaultValue = "1") Long page,
        @RequestParam(defaultValue = "10") Long size
    ){
        return groupTaskService.listGroupTasks(userId, groupId, page, size);
    }

    @GetMapping("/{groupId}/tasks/{taskId}")
    public GroupTaskResponse getGroupTaskDetail(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @PathVariable Long taskId
    ){
        return groupTaskService.getGroupTaskDetail(userId, groupId, taskId);
    }
}
