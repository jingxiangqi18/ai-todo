package com.qijx.aitodo.group.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.group.dto.GroupCreateRequest;
import com.qijx.aitodo.group.dto.GroupMemberResponse;
import com.qijx.aitodo.group.dto.GroupResponse;
import com.qijx.aitodo.group.service.GroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService){
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody GroupCreateRequest request
    ){
        return groupService.createGroup(userId, request);
    }

    @GetMapping
    public List<GroupResponse> listMyGroups(
        @AuthenticationPrincipal Long userId
    ){
        return groupService.listMyGroups(userId);
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroupDetail(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId
    ) {
        return groupService.getGroupDetail(userId, groupId);
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> listGroupMembers(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId
    ){
        return groupService.listGroupMembers(userId, groupId);
    }
    
}
