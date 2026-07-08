package com.qijx.aitodo.task.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.task.dto.TaskCreateRequest;
import com.qijx.aitodo.task.dto.TaskResponse;
import com.qijx.aitodo.task.dto.TaskStatusUpdateRequest;
import com.qijx.aitodo.task.dto.TaskUpdateRequest;
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

    public TaskResponse updateTask(Long userId, Long taskId, TaskUpdateRequest request){
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getUserId, userId)
        );

        if(task == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        if(request.getTitle() != null){
            if(request.getTitle().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务标题不得为空");
            }
            task.setTitle(request.getTitle());
        }

        if(request.getDescription() != null){
            task.setDescription(request.getDescription());
        }

        if(request.getPriority() != null){
            task.setPriority(resolvePriority(request.getPriority()));
        }

        if(request.getDueAt() != null){
            task.setDueAt(request.getDueAt());
        }

        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.updateById(task);

        return toResponse(task);
    }

    public TaskResponse updateTaskStatus(Long userId, Long taskId, TaskStatusUpdateRequest request){
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getUserId, userId)
        );

        if(task == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        task.setStatus(resolveStatus(request.getStatus()));
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.updateById(task);

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

    private String resolveStatus(String status){
        if(status == null || status.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务状态不能为空");
        }

        if(!status.equals("TODO") && !status.equals("IN_PROGRESS") && !status.equals("DONE")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务状态不正确");
        }

        return status;
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
