package com.qijx.aitodo.task.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.task.dto.TaskCreateRequest;
import com.qijx.aitodo.task.dto.TaskResponse;
import com.qijx.aitodo.task.service.TaskService;
import com.qijx.aitodo.user.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final JwtService jwtService;

    public TaskController(TaskService taskService, JwtService jwtService){
        this.taskService = taskService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody TaskCreateRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskService.createTask(userId, request);
    }
}
