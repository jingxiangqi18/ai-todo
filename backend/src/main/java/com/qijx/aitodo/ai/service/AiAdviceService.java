package com.qijx.aitodo.ai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qijx.aitodo.ai.dto.TaskAdviceResponse;
import com.qijx.aitodo.task.entity.Task;
import com.qijx.aitodo.task.entity.TaskStep;
import com.qijx.aitodo.task.mapper.TaskMapper;
import com.qijx.aitodo.task.mapper.TaskStepMapper;

@Service
public class AiAdviceService {
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
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final String chatUrl;
    private final String apiKey;
    private final String model;

    public AiAdviceService(
        TaskMapper taskMapper,
        TaskStepMapper taskStepMapper,
        @Value("${app.llm.chat-url}") String chatUrl,
        @Value("${app.llm.api-key}") String apiKey,
        @Value("${app.llm.model}") String model
    ){
        this.taskMapper = taskMapper;
        this.taskStepMapper = taskStepMapper;
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.model = model;

        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
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

        validateLlmConfiguration();

        String userPrompt = buildUserPrompt(
            message,
            tasks,
            stepsByTaskId
        );

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content", SYSTEM_PROMPT
                ),
                Map.of(
                    "role", "user",
                    "content", userPrompt
                )
            ),
            "thinking", Map.of(
                "type", "disabled"
            ),
            "stream", false
        );

        try{
            String requestJson = objectMapper.writeValueAsString(requestBody);

            String responseJson = restClient.post()
                .uri(chatUrl)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + apiKey
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(String.class);

            String advice = extractAdvice(responseJson);

            return createResponse(advice);
        }catch(RestClientException exception){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "调用AI服务失败");
        }catch(JsonProcessingException exception){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "处理AI服务数据失败");
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

    private String extractAdvice(String responseJson) throws JsonProcessingException {
        if(responseJson == null || responseJson.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务无返回内容");
        }

        JsonNode rootNode = objectMapper.readTree(responseJson);

        String advice = rootNode
            .path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText();

        if(advice.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务返回内容格式有误");
        }

        return advice;
    }

    private void validateLlmConfiguration(){
        if(chatUrl.isBlank() || apiKey.isBlank() || model.isBlank()){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "LLM服务尚未配置");
        }
    }

    private String valueOrDefault(Object value){
        if(value == null){
            return "未设置";
        }

        return value.toString();
    }

    private TaskAdviceResponse createResponse(String advice){
        TaskAdviceResponse response = new TaskAdviceResponse();

        response.setAdvice(advice);

        return response;
    }
}
