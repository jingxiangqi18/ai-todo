package com.qijx.aitodo.task.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.qijx.aitodo.task.dto.TaskCreateRequest;
import com.qijx.aitodo.task.dto.TaskPageResponse;
import com.qijx.aitodo.task.dto.TaskResponse;
import com.qijx.aitodo.task.dto.TaskStatsResponse;
import com.qijx.aitodo.task.dto.TaskStatusUpdateRequest;
import com.qijx.aitodo.task.dto.TaskUpdateRequest;
import com.qijx.aitodo.task.entity.Task;
import com.qijx.aitodo.task.mapper.TaskMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class TaskService {
    private final TaskMapper taskMapper;
    private static final String TASK_STATS_CACHE_KEY_PREFIX = "task:stats:";
    private static final Duration TASK_STATS_CACHE_TTL = Duration.ofMinutes(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaskService(
        TaskMapper taskMapper,
        StringRedisTemplate stringRedisTemplate
    ){
        this.taskMapper = taskMapper;
        this.stringRedisTemplate = stringRedisTemplate;
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

        invalidateTaskStatsCache(userId);

        return toResponse(task);
    }

    public TaskPageResponse listMyTasks(Long userId, String status, String priority, String keyword, Long page, Long size){
        if(page == null || page < 1){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "页码不能小于1");
        }

        if(size == null || size < 1 || size > 50){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每页数量应在1到50之间");
        }
        
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .orderByDesc(Task::getCreatedAt);

        if(status != null && !status.isBlank()){
            queryWrapper.eq(Task::getStatus, resolveStatus(status));
        }

        if(priority != null && !priority.isBlank()){
            queryWrapper.eq(Task::getPriority, resolvePriority(priority));
        }

        if(keyword != null && !keyword.isBlank()){
            String trimmedKeyword = keyword.trim();

            queryWrapper.and(wrapper -> wrapper
                    .like(Task::getTitle, trimmedKeyword)
                    .or()
                    .like(Task::getDescription, trimmedKeyword)
            );
        }

        Page<Task> taskPage = taskMapper.selectPage(new Page<>(page, size), queryWrapper);

        TaskPageResponse response = new TaskPageResponse();

        response.setRecords(
            taskPage.getRecords()
                    .stream()
                    .map(this::toResponse)
                    .toList()  
        );

        response.setPage(taskPage.getCurrent());
        response.setSize(taskPage.getSize());
        response.setTotal(taskPage.getTotal());
        response.setPages((taskPage.getPages()));

        return response;
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

        invalidateTaskStatsCache(userId);

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

        invalidateTaskStatsCache(userId);

        return toResponse(task);
    }

    public void deleteTask(Long userId, Long taskId){
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getUserId, userId)   
        );

        if(task == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        taskMapper.deleteById(taskId);

        invalidateTaskStatsCache(userId);
    }

    public TaskStatsResponse getTaskStats(Long userId){
        String cacheKey = TASK_STATS_CACHE_KEY_PREFIX + userId;

        TaskStatsResponse cachedResponse = readTaskStatsFromCache(cacheKey);

        if(cachedResponse != null){
            return cachedResponse;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        TaskStatsResponse response = new TaskStatsResponse();
        
        response.setTotal(countByUserId(userId));
        response.setTodo(countByStatus(userId, "TODO"));
        response.setInProgress(countByStatus(userId, "IN_PROGRESS"));
        response.setDone(countByStatus(userId, "DONE"));
        response.setHighPriority(countByPriority(userId, "HIGH"));
        response.setDueToday(countDueToday(userId, todayStart, tomorrowStart));
        response.setOverdue(countOverdue(userId, now));

        cacheTaskStats(cacheKey, response);

        return response;
    }

    public List<TaskResponse> listUpcomingReminders(Long userId, Long minutes){
        if(minutes == null || minutes < 1 || minutes > 1440){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提醒时间范围应在1-1440分钟之间");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(minutes);

        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .ne(Task::getStatus, "DONE")
                    .ge(Task::getDueAt, now)
                    .lt(Task::getDueAt, deadline)
                    .orderByAsc(Task::getDueAt)
        );

        return tasks.stream()
                .map(this::toResponse)
                .toList();
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

    private long countByUserId(Long userId){
        return taskMapper.selectCount(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
        );
    }

    private long countByStatus(Long userId, String status){
        return taskMapper.selectCount(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .eq(Task::getStatus, status)
        );
    }

    private long countByPriority(Long userId, String priority){
        return taskMapper.selectCount(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .eq(Task::getPriority, priority)
        );
    }

    private long countDueToday(Long userId, LocalDateTime todayStart, LocalDateTime tomorrowStart){
        return taskMapper.selectCount(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .ge(Task::getDueAt, todayStart)
                    .lt(Task::getDueAt, tomorrowStart)
        );
    }

    private long countOverdue(Long userId, LocalDateTime now){
        return taskMapper.selectCount(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .ne(Task::getStatus, "DONE")
                    .lt(Task::getDueAt, now)
        );
    }

    private void cacheTaskStats(
        String cacheKey,
        TaskStatsResponse response
    ){
        try{
            String responseJson = objectMapper.writeValueAsString(response);

            stringRedisTemplate.opsForValue().set(
                cacheKey,
                responseJson,
                TASK_STATS_CACHE_TTL
            );
        }catch(JsonProcessingException exception){
            LOGGER.warn("任务统计结果序列化失败，本次不写入缓存");
        }catch(DataAccessException exception){
            LOGGER.warn("Redis写入失败，本次只返回数据库结果");
        }
    }

    private void invalidateTaskStatsCache(Long userId){
        String cacheKey = TASK_STATS_CACHE_KEY_PREFIX + userId;

        invalidateTaskStatsCacheByKey(cacheKey);
    }

    private void invalidateTaskStatsCacheByKey(String cacheKey){
        try{
            stringRedisTemplate.delete(cacheKey);
        }catch(DataAccessException exception){
            LOGGER.warn("Redis缓存删除失败，将依靠TTL自动失效");
        }
    }

    private TaskStatsResponse readTaskStatsFromCache(String cacheKey){
        try{
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);

            if(cachedJson == null){
                return null;
            }

            return objectMapper.readValue(cachedJson, TaskStatsResponse.class);
        }catch(JsonProcessingException exception){
            LOGGER.warn("任务统计缓存无法解析，改为查询数据库");

            invalidateTaskStatsCacheByKey(cacheKey);

            return null;
        }catch(DataAccessException exception){
            LOGGER.warn("Redis读取失败，任务统计改为查询数据库");

            return null;
        }
    }
}
