package com.qijx.aitodo.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.ai.dto.TaskStepDraftResponse;
import com.qijx.aitodo.task.entity.TaskStep;
import com.qijx.aitodo.task.mapper.TaskMapper;
import com.qijx.aitodo.task.mapper.TaskStepMapper;
import com.qijx.aitodo.task.entity.Task;

@Service
public class AiTaskStepDraftService {
    private static final String SYSTEM_PROMPT = """
            你是一个任务拆解助手。

            请根据系统提供的任务信息和用户要求，将任务拆解成可以实际执行的步骤。

            要求：
            1.返回2到8个步骤。
            2.步骤必须具体、简短、可执行。
            3.每个步骤标题不得超过100个字符。
            4.步骤应按照合理的执行顺序排列
            5.不要重复生成已有的步骤
            6.不要修改已有的任务
            7.只生成步骤草稿，用户决定是否保存
            """;

    private final TaskMapper taskMapper;
    private final TaskStepMapper taskStepMapper;
    private final ChatClient chatClient;

    public AiTaskStepDraftService(TaskMapper taskMapper, TaskStepMapper taskStepMapper, ChatClient.Builder chatClientBuilder){
        this.taskMapper = taskMapper;
        this.taskStepMapper = taskStepMapper;
        this.chatClient = chatClientBuilder.build();
    }

    public TaskStepDraftResponse generateDraft(Long userId, Long taskId, String instruction){
        Task task = findUserTask(userId, taskId);

        List<TaskStep> existingSteps = taskStepMapper.selectList(
            new LambdaQueryWrapper<TaskStep>()
                    .eq(TaskStep::getTaskId, taskId)
                    .orderByAsc(TaskStep::getId)   
        );

        String userPrompt = buildUserPrompt(task, existingSteps, instruction);

        OpenAiChatOptions.Builder chatOptions = 
                OpenAiChatOptions.builder()
                        .maxTokens(500)
                        .extraBody(
                            Map.of(
                                "thinking",
                                Map.of(
                                    "type",
                                    "disabled"
                                )
                            )
                        );

        TaskStepDraftResponse response;

        try{
            response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .options(chatOptions)
                    .call()
                    .entity(TaskStepDraftResponse.class);
        }catch(RuntimeException exception){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "生成任务草稿失败", exception);
        }

        validateAndCleanResponse(response);

        return response;
    }

    private Task findUserTask(Long userId, Long taskId){
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getUserId, userId)       
        );

        if(task == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        return task;
    }

    private String buildUserPrompt(Task task, List<TaskStep> existingSteps, String instruction){
        StringBuilder prompt = new StringBuilder();

        prompt.append("需要拆解的任务：\n")
                .append("标题：")
                .append(task.getTitle())
                .append("\n描述：")
                .append(valueOrDefault(task.getDescription()))
                .append("\n优先级：")
                .append(task.getPriority())
                .append("\n截止时间：")
                .append(valueOrDefault(task.getDueAt()))
                .append("\n\n");

        prompt.append("用户的额外拆解要求：\n");

        if(instruction == null || instruction.isBlank()){
            prompt.append("无额外要求\n\n");
        }else{
            prompt.append(instruction.trim())
                    .append("\n\n");
        }

        prompt.append("当前任务已有步骤：\n");

        if(existingSteps.isEmpty()){
            prompt.append("暂无已有步骤\n");
        }else{
            for(TaskStep existingStep : existingSteps){
                prompt.append("- ")
                        .append(existingStep.getTitle())
                        .append("\n");
            }
        }

        return prompt.toString();
    }

    private void validateAndCleanResponse(TaskStepDraftResponse response){
        if(response == null || response.getSteps() == null){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务未返回有效的步骤草稿");
        }

        if(response.getSteps().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务返回的步骤草稿为空");
        }

        if(response.getSteps().size() > 10){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务返回的步骤数量过多");
        }

        List<String> cleanedSteps = new ArrayList<>();

        for(String step : response.getSteps()){
            if(step == null || step.isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务返回了空步骤");
            }

            String cleanedStep = step.trim();

            if(cleanedStep.length() > 100){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务返回的步骤标题过长");
            }

            cleanedSteps.add(cleanedStep);
        }

        response.setSteps(cleanedSteps);
    }

    private String valueOrDefault(Object value){
        if(value == null){
            return "未设置";
        }

        return value.toString();
    }
}
