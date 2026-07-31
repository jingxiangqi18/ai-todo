package com.qijx.aitodo.task.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.qijx.aitodo.task.dto.TaskStepCreateRequest;
import com.qijx.aitodo.task.dto.TaskStepResponse;
import com.qijx.aitodo.task.dto.TaskStepUpdateRequest;
import com.qijx.aitodo.task.service.TaskStepService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks/{taskId}/steps")
public class TaskStepController {
    private final TaskStepService taskStepService;

    public TaskStepController(TaskStepService taskStepService){
        this.taskStepService = taskStepService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskStepResponse createStep(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long taskId,
        @Valid @RequestBody TaskStepCreateRequest request
    ){
        return taskStepService.createTaskStep(userId, taskId, request);
    }

    @GetMapping
    public List<TaskStepResponse> listTaskSteps(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long taskId
    ){
        return taskStepService.listTaskSteps(userId, taskId);
    }

    @PatchMapping("/{stepId}")
    public TaskStepResponse updateTaskStep(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long taskId,
        @PathVariable Long stepId,
        @Valid @RequestBody TaskStepUpdateRequest request
    ){
        return taskStepService.updateTaskStep(userId, taskId, stepId, request);
    }

    @DeleteMapping("/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskStep(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long taskId,
        @PathVariable Long stepId
    ){
        taskStepService.deleteTaskStep(userId, taskId, stepId);
    }
}
