package com.qijx.aitodo.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvitationCreateRequest {
    @NotBlank(message = "被邀请用户账号不能为空")
    @Size(max = 100, message = "被邀请用户账号不能超过100个字符")
    private String account;
}
