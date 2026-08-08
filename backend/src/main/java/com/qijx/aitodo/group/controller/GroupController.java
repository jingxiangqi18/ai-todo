package com.qijx.aitodo.group.controller;

import com.qijx.aitodo.group.service.GroupInvitationService;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.qijx.aitodo.group.dto.InvitationCreateRequest;
import com.qijx.aitodo.group.dto.InvitationResponse;
import com.qijx.aitodo.group.service.GroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupInvitationService groupInvitationService;
    private final GroupService groupService;

    public GroupController(GroupService groupService, GroupInvitationService groupInvitationService){
        this.groupService = groupService;
        this.groupInvitationService = groupInvitationService;
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

    @PostMapping("/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse createInvitation(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId,
        @Valid @RequestBody InvitationCreateRequest request
    ){
        String account = request.getAccount();

        return groupInvitationService.createInvitation(userId, groupId, account);
    }

    @DeleteMapping("/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveGroup(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long groupId
    ){
        groupService.leaveGroup(userId, groupId);
    }
}
