package com.qijx.aitodo.group.controller;

import com.qijx.aitodo.group.service.GroupInvitationService;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.aitodo.group.dto.GroupResponse;
import com.qijx.aitodo.group.dto.InvitationResponse;

@RestController
@RequestMapping("/api/group-invitations")
public class GroupInvitationController {
    private final GroupInvitationService groupInvitationService;

    public GroupInvitationController(GroupInvitationService groupInvitationService){
        this.groupInvitationService = groupInvitationService;
    }

    @GetMapping("/pending")
    public List<InvitationResponse> listPendingInvitations(
        @AuthenticationPrincipal Long inviteeId
    ){
        return groupInvitationService.listPendingInvitations(inviteeId);
    }

    @PostMapping("/{invitationId}/accept")
    public GroupResponse acceptInvitation(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long invitationId
    ){
        return groupInvitationService.acceptInvitation(userId, invitationId);
    }

    @PostMapping("/{invitationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectInvitation(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long invitationId
    ){
        groupInvitationService.rejectInvitation(userId, invitationId);
    }
}
