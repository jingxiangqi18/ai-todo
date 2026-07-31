package com.qijx.aitodo.task.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.task.dto.TaskStepBatchCreateRequest;
import com.qijx.aitodo.task.dto.TaskStepCreateRequest;
import com.qijx.aitodo.task.dto.TaskStepResponse;
import com.qijx.aitodo.task.dto.TaskStepUpdateRequest;
import com.qijx.aitodo.task.entity.Task;
import com.qijx.aitodo.task.entity.TaskStep;
import com.qijx.aitodo.task.mapper.TaskMapper;
import com.qijx.aitodo.task.mapper.TaskStepMapper;

@Service
public class TaskStepService {
    private final TaskMapper taskMapper;

    private final TaskStepMapper taskStepMapper;

    public TaskStepService(TaskMapper taskMapper, TaskStepMapper taskStepMapper){
        this.taskMapper = taskMapper;
        this.taskStepMapper = taskStepMapper;
    }

    public TaskStepResponse createTaskStep(Long userId, Long taskId, TaskStepCreateRequest request){
        ensureTaskBelongsToUser(userId, taskId);

        LocalDateTime now = LocalDateTime.now();

        TaskStep taskStep = new TaskStep();

        taskStep.setTaskId(taskId);
        taskStep.setTitle(request.getTitle());
        taskStep.setCompleted(false);
        taskStep.setCreatedAt(now);
        taskStep.setUpdatedAt(now);

        taskStepMapper.insert(taskStep);

        return toResponse(taskStep);
    }

    @Transactional
    public List<TaskStepResponse> createTaskStepsBatch(Long userId, Long taskId, TaskStepBatchCreateRequest request){
        ensureTaskBelongsToUser(userId, taskId);

        List<String> noramlizedTitles = normalizeAndValidateBatchTitles(request.getTitles());

        ensureTitlesDoNotAlreadyExist(taskId, noramlizedTitles);

        LocalDateTime now = LocalDateTime.now();

        List<TaskStepResponse> responses = new ArrayList<>();

        for(String title : noramlizedTitles){
            TaskStep taskStep = new TaskStep();

            taskStep.setTaskId(taskId);
            taskStep.setTitle(title);
            taskStep.setCompleted(false);
            taskStep.setCreatedAt(now);
            taskStep.setUpdatedAt(now);

            int insertedRows = taskStepMapper.insert(taskStep);

            if(insertedRows != 1){
                throw new IllegalStateException("批量创建任务步骤失败");
            }

            responses.add(toResponse(taskStep));
        }

        return responses;
    }

    public List<TaskStepResponse> listTaskSteps(Long userId, Long taskId){
        ensureTaskBelongsToUser(userId, taskId);

        List<TaskStep> taskSteps = taskStepMapper.selectList(
            new LambdaQueryWrapper<TaskStep>()
                    .eq(TaskStep::getTaskId, taskId)
                    .orderByAsc(TaskStep::getId)
        );

        return taskSteps.stream()
            .map(this::toResponse)
            .toList();
    }

    public TaskStepResponse updateTaskStep(Long userId, Long taskId, Long stepId, TaskStepUpdateRequest request){
        ensureTaskBelongsToUser(userId, taskId);

        TaskStep taskStep = getTaskStep(taskId, stepId);

        if(request.getTitle() == null && request.getCompleted() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少需要修改一个字段");
        }

        if(request.getTitle() != null){
            if(request.getTitle().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "步骤标题不能为空");
            }

            taskStep.setTitle(request.getTitle());
        }

        if(request.getCompleted() != null){
            taskStep.setCompleted(request.getCompleted());
        }

        taskStep.setUpdatedAt(LocalDateTime.now());

        taskStepMapper.updateById(taskStep);

        return toResponse(taskStep);
    }

    public void deleteTaskStep(Long userId, Long taskId, Long taskStepId){
        ensureTaskBelongsToUser(userId, taskId);

        TaskStep taskStep = getTaskStep(taskId, taskStepId);

        taskStepMapper.deleteById(taskStep.getId());
    }

    private void ensureTaskBelongsToUser(Long userId, Long taskId){
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getUserId, userId)
        );

        if(task == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }
    }

    private TaskStepResponse toResponse(TaskStep taskStep){
        TaskStepResponse response = new TaskStepResponse();

        response.setId(taskStep.getId());
        response.setTitle(taskStep.getTitle());
        response.setCompleted(taskStep.getCompleted());
        response.setCreatedAt(taskStep.getCreatedAt());
        response.setUpdatedAt(taskStep.getUpdatedAt());

        return response;
    }

    private TaskStep getTaskStep(Long taskId, Long taskStepId){
        TaskStep taskStep = taskStepMapper.selectOne(
            new LambdaQueryWrapper<TaskStep>()
                    .eq(TaskStep::getId, taskStepId)
                    .eq(TaskStep::getTaskId, taskId)   
        );

        if(taskStep == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务步骤不存在");
        }

        return taskStep;
    }

    private List<String> normalizeAndValidateBatchTitles(List<String> titles){
        if(titles == null || titles.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少需要选择一个步骤");
        }

        if(titles.size() > 10){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "一次最多保存10个步骤");
        }

        List<String> normalizedTitles = new ArrayList<>();
        Set<String> uniqueTitles = new HashSet<>();

        for(String title : titles){
            if(title == null || title.isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "步骤标题不能为空");
            }

            String normalizedTitle = title.trim();

            if(!uniqueTitles.add(normalizedTitle)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能保存重复的步骤");
            }

            normalizedTitles.add(normalizedTitle);
        }

        return normalizedTitles;
    }

    private void ensureTitlesDoNotAlreadyExist(Long taskId, List<String> newTitles){
        List<TaskStep> existingSteps = taskStepMapper.selectList(
            new LambdaQueryWrapper<TaskStep>()
                    .eq(TaskStep::getTaskId, taskId)    
        );

        Set<String> existingTitles = new HashSet<>();

        for(TaskStep existingStep : existingSteps){
            existingTitles.add(existingStep.getTitle().trim());
        }

        for(String newTitle : newTitles){
            if(existingTitles.contains(newTitle)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "步骤已存在：" + newTitle);
            }
        }
    }
}
