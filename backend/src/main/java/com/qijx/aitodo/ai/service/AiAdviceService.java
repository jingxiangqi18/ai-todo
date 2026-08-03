package com.qijx.aitodo.ai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.ai.dto.TaskAdviceResponse;
import com.qijx.aitodo.task.entity.Task;
import com.qijx.aitodo.task.entity.TaskStep;
import com.qijx.aitodo.task.mapper.TaskMapper;
import com.qijx.aitodo.task.mapper.TaskStepMapper;

@Service
public class AiAdviceService {
    private static final String FEATURE = "TASK_ADVICE";

    private static final String SYSTEM_PROMPT = """
            你是一个任务安排助手

            请根据用户输入的自然语言描述，以及系统提供的未完成任务情况来推荐用户目前适合处理的任务或任务步骤安排。

            要求：
            1. 用户未必会提供当前的精力、空闲时间等信息，上述信息并不强制，但如果提供了需要进行参考。
            2. 综合考虑截止时间、优先级和任务状态。
            3. 临近截止、优先级高、正在进行中的任务可以优先关注。
            4. 不允许编造不存在的任务或步骤。
            5. 一次不一定需要将所有任务和步骤都规划完，可以推荐3个以内的任务或步骤，并配合简明的理由。
            6. 使用简洁自然的中文输出回答。
            """;

    private final TaskMapper taskMapper;
    private final TaskStepMapper taskStepMapper;
    private final ChatClient chatClient;
    private final AiCallLogService aiCallLogService;

    public AiAdviceService(
        TaskMapper taskMapper,
        TaskStepMapper taskStepMapper,
        ChatClient.Builder chatClientBuilder,
        AiCallLogService aiCallLogService
    ){
        this.taskMapper = taskMapper;
        this.taskStepMapper = taskStepMapper;
        this.chatClient = chatClientBuilder.build();
        this.aiCallLogService = aiCallLogService;
    }

    public TaskAdviceResponse generateAdvice(Long userId, String message){
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .in(
                        Task::getStatus,
                        List.of("TODO", "IN_PROGRESS")
                    )
        );

        if(tasks.isEmpty()){
            return createResponse(
                "你当前没有未完成的任务，可以休息一下或选择创建新任务"
            );
        }

        List<Long> taskIds = tasks.stream()
            .map(Task::getId)
            .toList();

        List<TaskStep> unfinishedSteps = taskStepMapper.selectList(
            new LambdaQueryWrapper<TaskStep>()
                    .in(TaskStep::getTaskId, taskIds)
                    .eq(TaskStep::getCompleted, false)
                    .orderByAsc(TaskStep::getTaskId)
                    .orderByAsc(TaskStep::getId)   
        );

        Map<Long, List<TaskStep>> stepsByTaskId =
            unfinishedSteps.stream()
                .collect(
                    Collectors.groupingBy(TaskStep::getTaskId)
                );

        String userPrompt = buildUserPrompt(
            message,
            tasks,
            stepsByTaskId
        );

        OpenAiChatOptions.Builder chatOptions = 
            OpenAiChatOptions.builder()
                .maxTokens(400)
                .extraBody(
                    Map.of(
                        "thinking",
                        Map.of(
                            "type",
                            "disabled"
                        )
                    )
                );
        
        long startedAt  = System.nanoTime();

        try{
            ChatResponse chatResponse = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .options(chatOptions)
                    .call()
                    .chatResponse();

            String advice = extractAdvice(chatResponse);

            if(advice == null || advice.isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务无返回内容");
            }

            long durationMs = calculateDurationMs(startedAt);

            aiCallLogService.recordSuccess(userId, FEATURE, chatResponse, durationMs);

            return createResponse(advice.trim());
        }catch(RuntimeException exception){
            long durationMs = calculateDurationMs(startedAt);

            aiCallLogService.recordFailure(userId, FEATURE, durationMs, exception);

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "调用AI服务失败", exception);
        }
    }

        private String buildUserPrompt(
        String message,
        List<Task> tasks,
        Map<Long, List<TaskStep>> stepsByTaskId
    ){
        StringBuilder prompt = new StringBuilder();

        prompt.append("当前时间：")
            .append(LocalDateTime.now())
            .append("\n\n");

        prompt.append("用户输入：\n")
            .append(message)
            .append("\n\n");

        prompt.append("用户当前未完成的任务：\n");

        for(Task task : tasks){
            prompt.append("\n任务ID：")
                .append(task.getId())
                .append("\n标题：")
                .append(task.getTitle())
                .append("\n描述：")
                .append(task.getDescription())
                .append("\n状态：")
                .append(task.getStatus())
                .append("\n优先级：")
                .append(task.getPriority())
                .append("\n截止时间：")
                .append(valueOrDefault(task.getDueAt()))
                .append("\n未完成步骤：\n");

            List<TaskStep> taskSteps =
                stepsByTaskId.getOrDefault(task.getId(), List.of());
            
            if(taskSteps.isEmpty()){
                prompt.append("- 暂无未完成步骤\n");
            }else{
                for(TaskStep taskStep : taskSteps){
                    prompt.append("- 步骤ID：")
                        .append(taskStep.getId())
                        .append("，内容：")
                        .append(taskStep.getTitle())
                        .append("\n");
                }
            }
        }

        return prompt.toString();
    }

    private String valueOrDefault(Object value) {
        if (value == null) {
            return "未设置";
        }

        return value.toString();
    }

    private TaskAdviceResponse createResponse(String advice){
        TaskAdviceResponse response = new TaskAdviceResponse();

        response.setAdvice(advice);

        return response;
    }

    private String extractAdvice(ChatResponse chatResponse){
        if(chatResponse == null){
            return null;
        }

        if(chatResponse.getResult() == null){
            return null;
        }

        if(chatResponse.getResult().getOutput() == null){
            return null;
        }

        return chatResponse.getResult().getOutput().getText();
    }

    private long calculateDurationMs(long startedAt){
        long elapsedNanos = System.nanoTime() - startedAt;

        return elapsedNanos / 1_000_000;
    }
}
