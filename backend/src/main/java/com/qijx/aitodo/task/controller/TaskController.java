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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.task.dto.TaskCreateRequest;
import com.qijx.aitodo.task.dto.TaskPageResponse;
import com.qijx.aitodo.task.dto.TaskResponse;
import com.qijx.aitodo.task.dto.TaskStatsResponse;
import com.qijx.aitodo.task.dto.TaskStatusUpdateRequest;
import com.qijx.aitodo.task.dto.TaskUpdateRequest;
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

    @GetMapping
    public TaskPageResponse listMyTasks(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") Long page,
        @RequestParam(defaultValue = "10") Long size
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskService.listMyTasks(userId, status, priority, keyword, page, size);
    }

    @GetMapping("/stats")
    public TaskStatsResponse getTaskStats(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskService.getTaskStats(userId);
    }

    @GetMapping("/{id}")
    public TaskResponse getMyTask(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskService.getMyTask(userId, id);
    }

    @PatchMapping("/{id}")
    public TaskResponse updateTask(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @Valid @RequestBody TaskUpdateRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskService.updateTask(userId, id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @Valid @RequestBody TaskStatusUpdateRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskService.updateTaskStatus(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public void deteleTask(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        taskService.deleteTask(userId, id);
    }

    @GetMapping("/reminders")
    public List<TaskResponse> listUpcomingReminders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(defaultValue = "60") Long minutes
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return taskService.listUpcomingReminders(userId, minutes);
    }
}
