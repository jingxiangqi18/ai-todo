package com.qijx.aitodo.ai.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.ai.dto.TaskAdviceRequest;
import com.qijx.aitodo.ai.dto.TaskAdviceResponse;
import com.qijx.aitodo.ai.dto.TaskStepDraftRequest;
import com.qijx.aitodo.ai.dto.TaskStepDraftResponse;
import com.qijx.aitodo.ai.service.AiAdviceService;
import com.qijx.aitodo.user.service.JwtService;
import com.qijx.aitodo.ai.service.AiRateLimitService;
import com.qijx.aitodo.ai.service.AiTaskStepDraftService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiAdviceController {
    private final AiAdviceService aiAdviceService;
    private final JwtService jwtService;
    private final AiRateLimitService aiRateLimitService;
    private final AiTaskStepDraftService aiTaskStepDraftService;

    public AiAdviceController(AiAdviceService aiAdviceService, JwtService jwtService, AiRateLimitService aiRateLimitService, AiTaskStepDraftService aiTaskStepDraftService){
        this.aiAdviceService = aiAdviceService;
        this.jwtService = jwtService;
        this.aiRateLimitService = aiRateLimitService;
        this.aiTaskStepDraftService = aiTaskStepDraftService;
    }

    @PostMapping("/task-advice")
    public TaskAdviceResponse getTaskAdvice(
        @RequestHeader(value = "Authorization") String authorizationHeader,
        @Valid @RequestBody TaskAdviceRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        aiRateLimitService.checkRateLimit(userId);
        
        return aiAdviceService.generateAdvice(userId, request.getMessage());
    }

    @PostMapping("/tasks/{taskId}/step-drafts")
    public TaskStepDraftResponse generateTaskTaskStepDraft(
        @RequestHeader(value = "Authorization") String authorizaitonHeader,
        @PathVariable Long taskId,
        @Valid @RequestBody TaskStepDraftRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizaitonHeader);

        aiRateLimitService.checkRateLimit(userId);

        return aiTaskStepDraftService.generateDraft(userId, taskId, request.getInstruction());
    }
}
