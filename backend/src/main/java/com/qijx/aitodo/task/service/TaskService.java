package com.qijx.aitodo.task.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.task.dto.TaskCreateRequest;
import com.qijx.aitodo.task.dto.TaskResponse;
import com.qijx.aitodo.task.entity.Task;
import com.qijx.aitodo.task.mapper.TaskMapper;

@Service
public class TaskService {
    private final TaskMapper taskMapper;

    public TaskService(TaskMapper taskMapper){
        this.taskMapper = taskMapper;
    }

    public TaskResponse createTask(Long userId, TaskCreateRequest request){
        LocalDateTime now = LocalDateTime.now();

        Task task = new Task();

        task.setUserId(userId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus("TODO");
        task.setPriority(resolvePriority(request.getPriority()));
        task.setDueAt(request.getDueAt());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        taskMapper.insert(task);

        return toResponse(task);
    }

    public List<TaskResponse> listMyTasks(Long userId){
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .orderByDesc(Task::getCreatedAt)
        );

        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getMyTask(Long userId, Long taskId){
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getUserId, userId)
        );

        if(task == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        return toResponse(task);
    }

    private String resolvePriority(String priority){
        if(priority == null || priority.isBlank()){
            return "MEDIUM";
        }

        if(!priority.equals("LOW") && !priority.equals("MEDIUM") && !priority.equals("HIGH")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务优先级有误");
        }

        return priority;
    }

    private TaskResponse toResponse(Task task){
        TaskResponse response = new TaskResponse();

        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDueAt(task.getDueAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        return response;
    }
}
