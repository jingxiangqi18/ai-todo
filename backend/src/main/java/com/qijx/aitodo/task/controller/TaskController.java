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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.qijx.aitodo.task.dto.TaskCreateRequest;
import com.qijx.aitodo.task.dto.TaskPageResponse;
import com.qijx.aitodo.task.dto.TaskResponse;
import com.qijx.aitodo.task.dto.TaskStatsResponse;
import com.qijx.aitodo.task.dto.TaskStatusUpdateRequest;
import com.qijx.aitodo.task.dto.TaskUpdateRequest;
import com.qijx.aitodo.task.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService){
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody TaskCreateRequest request
    ){
        return taskService.createTask(userId, request);
    }

    @GetMapping
    public TaskPageResponse listMyTasks(
        @AuthenticationPrincipal Long userId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") Long page,
        @RequestParam(defaultValue = "10") Long size
    ){
        return taskService.listMyTasks(userId, status, priority, keyword, page, size);
    }

    @GetMapping("/stats")
    public TaskStatsResponse getTaskStats(
        @AuthenticationPrincipal Long userId
    ){
        return taskService.getTaskStats(userId);
    }

    @GetMapping("/{id}")
    public TaskResponse getMyTask(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long id
    ){
        return taskService.getMyTask(userId, id);
    }

    @PatchMapping("/{id}")
    public TaskResponse updateTask(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long id,
        @Valid @RequestBody TaskUpdateRequest request
    ){
        return taskService.updateTask(userId, id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long id,
        @Valid @RequestBody TaskStatusUpdateRequest request
    ){
        return taskService.updateTaskStatus(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long id
    ){
        taskService.deleteTask(userId, id);
    }

    @GetMapping("/reminders")
    public List<TaskResponse> listUpcomingReminders(
        @AuthenticationPrincipal Long userId,
        @RequestParam(defaultValue = "60") Long minutes
    ){
        return taskService.listUpcomingReminders(userId, minutes);
    }
}
