package com.qijx.aitodo.ai.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.qijx.aitodo.ai.dto.TaskAdviceRequest;
import com.qijx.aitodo.ai.dto.TaskAdviceResponse;
import com.qijx.aitodo.ai.dto.TaskStepDraftRequest;
import com.qijx.aitodo.ai.dto.TaskStepDraftResponse;
import com.qijx.aitodo.ai.service.AiAdviceService;
import com.qijx.aitodo.ai.service.AiRateLimitService;
import com.qijx.aitodo.ai.service.AiTaskStepDraftService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiAdviceController {
    private final AiAdviceService aiAdviceService;
    private final AiRateLimitService aiRateLimitService;
    private final AiTaskStepDraftService aiTaskStepDraftService;

    public AiAdviceController(AiAdviceService aiAdviceService, AiRateLimitService aiRateLimitService, AiTaskStepDraftService aiTaskStepDraftService){
        this.aiAdviceService = aiAdviceService;
        this.aiRateLimitService = aiRateLimitService;
        this.aiTaskStepDraftService = aiTaskStepDraftService;
    }

    @PostMapping("/task-advice")
    public TaskAdviceResponse getTaskAdvice(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody TaskAdviceRequest request
    ){
        aiRateLimitService.checkRateLimit(userId);
        
        return aiAdviceService.generateAdvice(userId, request.getMessage());
    }

    @PostMapping("/tasks/{taskId}/step-drafts")
    public TaskStepDraftResponse generateTaskTaskStepDraft(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long taskId,
        @Valid @RequestBody TaskStepDraftRequest request
    ){
        aiRateLimitService.checkRateLimit(userId);

        return aiTaskStepDraftService.generateDraft(userId, taskId, request.getInstruction());
    }
}
