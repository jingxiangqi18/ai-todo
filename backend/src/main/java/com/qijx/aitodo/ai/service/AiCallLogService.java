package com.qijx.aitodo.ai.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.qijx.aitodo.ai.entity.AiCallLog;
import com.qijx.aitodo.ai.mapper.AiCallLogMapper;

@Service
public class AiCallLogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiCallLogService.class);

    private final AiCallLogMapper aiCallLogMapper;
    private final String configuredModel;

    public AiCallLogService(AiCallLogMapper aiCallLogMapper, @Value("${spring.ai.openai.chat.model}") String configuredModel){
        this.aiCallLogMapper = aiCallLogMapper;
        this.configuredModel = configuredModel;
    }

    public void recordSuccess(Long userId, String feature, ChatResponse chatResponse, long durationMs){
        AiCallLog callLog = createBaseLog(userId, feature, durationMs);
        callLog.setSuccess(true);

        fillResponseMetadata(callLog, chatResponse);
        saveSafely(callLog);
    }

    public void recordFailure(Long userId, String feature, long durationMs, RuntimeException exception){
        AiCallLog callLog = createBaseLog(userId, feature, durationMs);
        callLog.setSuccess(false);
        callLog.setErrorMessage(createErrorMessage(exception));

        saveSafely(callLog);
    }

    private AiCallLog createBaseLog(Long userId, String feature, long durationMs){
        AiCallLog callLog = new AiCallLog();

        callLog.setUserId(userId);
        callLog.setFeature(feature);
        callLog.setModel(configuredModel);
        callLog.setPromptTokens(0);
        callLog.setCompletionTokens(0);
        callLog.setTotalTokens(0);
        callLog.setDurationMs(durationMs);
        callLog.setCreatedAt(LocalDateTime.now());

        return callLog;
    }

    private void fillResponseMetadata(AiCallLog callLog, ChatResponse chatResponse){
        if(chatResponse == null){
            return;
        }

        ChatResponseMetadata metadata = chatResponse.getMetadata();

        if(metadata == null){
            return;
        }

        if(metadata.getModel() != null && !metadata.getModel().isBlank()){
            callLog.setModel(metadata.getModel());
        }

        Usage usage = metadata.getUsage();

        if(usage == null){
            return;
        }

        if(usage.getPromptTokens() != null){
            callLog.setPromptTokens(usage.getPromptTokens());
        }

        if(usage.getCompletionTokens() != null){
            callLog.setCompletionTokens(usage.getCompletionTokens());
        }

        if(usage.getTotalTokens() != null){
            callLog.setTotalTokens(usage.getTotalTokens());
        }
    }

    private String createErrorMessage(RuntimeException exception){
        String message = exception.getMessage();

        if(message == null || message.isBlank()){
            message = exception.getClass().getSimpleName();
        }

        if(message.length() > 300){
            return message.substring(0, 300);
        }

        return message;
    }

    private void saveSafely(AiCallLog callLog){
        try{
            aiCallLogMapper.insert(callLog);
        }catch(RuntimeException exception){
            LOGGER.error("保存AI调用日志失败", exception);
        }
    }
}
