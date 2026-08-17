package com.qijx.aitodo.group.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.group.dto.GroupTaskAssigneeUpdateRequest;
import com.qijx.aitodo.group.dto.GroupTaskCreateRequest;
import com.qijx.aitodo.group.dto.GroupTaskResponse;
import com.qijx.aitodo.group.dto.GroupTaskStatusUpdateRequest;
import com.qijx.aitodo.group.dto.GroupTaskUpdateRequest;
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

    @PatchMapping("/{groupId}/tasks/{taskId}")
    public GroupTaskResponse updateGroupTask(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @PathVariable Long taskId,
        @Valid @RequestBody GroupTaskUpdateRequest request
    ){
        return groupTaskService.updateGroupTask(userId, groupId, taskId, request);
    }

    @PatchMapping("/{groupId}/tasks/{taskId}/status")
    public GroupTaskResponse updateGroupTaskStatus(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @PathVariable Long taskId,
        @Valid @RequestBody GroupTaskStatusUpdateRequest request
    ){
        return groupTaskService.updateGroupTaskStatus(userId, groupId, taskId, request);
    }

    @PatchMapping("/{groupId}/tasks/{taskId}/assignee")
    public GroupTaskResponse updateGroupTaskAssignee(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @PathVariable Long taskId,
        @Valid @RequestBody GroupTaskAssigneeUpdateRequest request
    ){
        return groupTaskService.updateGroupTaskAssignee(userId, groupId, taskId, request);
    }

    @DeleteMapping("/{groupId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupTask(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @PathVariable Long taskId
    ){
        groupTaskService.deleteGroupTask(userId, groupId, taskId);
    }
}
