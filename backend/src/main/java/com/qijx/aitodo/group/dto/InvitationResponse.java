package com.qijx.aitodo.group.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InvitationResponse {
    private Long id;

    private Long groupId;

    private String groupName;

    private Long inviterId;

    private String inviterName;

    private Long inviteeId;

    private String inviteeName;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
