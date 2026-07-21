package com.qijx.aitodo.ai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.ai.dto.TaskAdviceRequest;
import com.qijx.aitodo.ai.dto.TaskAdviceResponse;
import com.qijx.aitodo.ai.service.AiAdviceService;
import com.qijx.aitodo.user.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiAdviceController {
    private final AiAdviceService aiAdviceService;
    private final JwtService jwtService;

    public AiAdviceController(AiAdviceService aiAdviceService, JwtService jwtService){
        this.aiAdviceService = aiAdviceService;
        this.jwtService = jwtService;
    }

    @PostMapping("/task-advice")
    public TaskAdviceResponse getTaskAdvice(
        @RequestHeader(value = "Authorization") String authorizationHeader,
        @Valid @RequestBody TaskAdviceRequest request
    ){
        Long userId = jwtService.parseUserIdFromAuthorizationHeader(authorizationHeader);

        return aiAdviceService.generateAdvice(userId, request.getMessage());
    }
}
