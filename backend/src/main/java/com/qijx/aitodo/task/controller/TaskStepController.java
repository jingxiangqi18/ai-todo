package com.qijx.aitodo.task.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.task.dto.TaskStepCreateRequest;
import com.qijx.aitodo.task.dto.TaskStepResponse;
import com.qijx.aitodo.task.dto.TaskStepUpdateRequest;
import com.qijx.aitodo.task.service.TaskStepService;
import com.qijx.aitodo.user.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks/{taskId}/steps")
public class TaskStepController {
    private final TaskStepService taskStepService;
    private final JwtService jwtService;

    public TaskStepController(TaskStepService taskStepService, JwtService jwtService){
        this.taskStepService = taskStepService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskStepResponse createStep(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long taskId,
        @Valid @RequestBody TaskStepCreateRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskStepService.createTaskStep(userId, taskId, request);
    }

    @GetMapping
    public List<TaskStepResponse> listTaskSteps(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long taskId
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskStepService.listTaskSteps(userId, taskId);
    }

    @PatchMapping("/{stepId}")
    public TaskStepResponse updateTaskStep(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long taskId,
        @PathVariable Long stepId,
        @Valid @RequestBody TaskStepUpdateRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskStepService.updateTaskStep(userId, taskId, stepId, request);
    }

    @DeleteMapping("/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskStep(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long taskId,
        @PathVariable Long stepId
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        taskStepService.deleteTaskStep(userId, taskId, stepId);
    }
}
