package com.qijx.aitodo.group.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.group.dto.GroupResponse;
import com.qijx.aitodo.group.dto.InvitationResponse;
import com.qijx.aitodo.group.entity.TaskGroup;
import com.qijx.aitodo.group.entity.TaskGroupInvitation;
import com.qijx.aitodo.group.entity.TaskGroupMember;
import com.qijx.aitodo.group.mapper.TaskGroupInvitationMapper;
import com.qijx.aitodo.group.mapper.TaskGroupMapper;
import com.qijx.aitodo.group.mapper.TaskGroupMemberMapper;
import com.qijx.aitodo.user.entity.User;
import com.qijx.aitodo.user.mapper.UserMapper;

@Service
public class GroupInvitationService {
    private final TaskGroupMemberMapper taskGroupMemberMapper;
    private final TaskGroupInvitationMapper taskGroupInvitationMapper;
    private final TaskGroupMapper taskGroupMapper;
    private final UserMapper userMapper;

    public GroupInvitationService(
        TaskGroupMemberMapper taskGroupMemberMapper,
        UserMapper userMapper,
        TaskGroupInvitationMapper taskGroupInvitationMapper,
        TaskGroupMapper taskGroupMapper
    ){
        this.taskGroupMemberMapper = taskGroupMemberMapper;
        this.userMapper = userMapper;
        this.taskGroupInvitationMapper = taskGroupInvitationMapper;
        this.taskGroupMapper = taskGroupMapper;
    }

    public InvitationResponse createInvitation(Long inviterId, Long groupId, String account){
        TaskGroup group = taskGroupMapper.selectOne(
            new LambdaQueryWrapper<TaskGroup>()
                    .eq(TaskGroup::getId, groupId)
        );

        if(group == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工作组不存在");
        }

        ensureOwnerOrAdmin(group, inviterId);

        User invitee = findInviteeByAccount(account);
        User inviter = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                    .eq(User::getId, inviterId)
        );

        if(!"ACTIVE".equals(invitee.getStatus())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "被邀请用户被禁用");
        }

        if(invitee.getId().equals(inviterId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能邀请自己进入自己的工作组");
        }

        ensureNotMember(groupId, invitee.getId());

        ensureNoPendingInvitation(groupId, invitee.getId());

        TaskGroupInvitation invitation = new TaskGroupInvitation();

        LocalDateTime now = LocalDateTime.now();

        invitation.setGroupId(groupId);
        invitation.setInviterId(inviterId);
        invitation.setInviteeId(invitee.getId());
        invitation.setStatus("PENDING");
        invitation.setCreatedAt(now);

        int insertedRows = taskGroupInvitationMapper.insert(invitation);

        if(insertedRows != 1){
            throw new IllegalStateException("创建小组邀请失败");
        }

        InvitationResponse response = toResponse(invitation, group, inviter, invitee);

        return response;
    }

    public List<InvitationResponse> listPendingInvitations(Long inviteeId){
        return taskGroupInvitationMapper.selectPendingInvitationResponse(inviteeId);
    }

    @Transactional
    public GroupResponse acceptInvitation(Long userId, Long invitationId){
        TaskGroupInvitation invitation = taskGroupInvitationMapper.selectOne(
            new LambdaQueryWrapper<TaskGroupInvitation>()
                    .eq(TaskGroupInvitation::getId, invitationId)
                    .eq(TaskGroupInvitation::getInviteeId, userId)
                    .eq(TaskGroupInvitation::getStatus, "PENDING")
        );

        if(invitation == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该邀请不存在");
        }

        TaskGroup group = taskGroupMapper.selectOne(
            new LambdaQueryWrapper<TaskGroup>()
                    .eq(TaskGroup::getId, invitation.getGroupId())
        );

        if(group == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该工作组不存在");
        }

        Long membershipCount = taskGroupMemberMapper.selectCount(
            new LambdaQueryWrapper<TaskGroupMember>()
                    .eq(TaskGroupMember::getUserId, userId)
                    .eq(TaskGroupMember::getGroupId, group.getId())
        );

        if(membershipCount > 0){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "你已在该工作组中");
        }

        int updated = taskGroupInvitationMapper.acceptInvitation(invitationId, userId);

        if(updated != 1){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邀请已被处理");
        }

        TaskGroupMember member = new TaskGroupMember();

        LocalDateTime now = LocalDateTime.now();

        member.setGroupId(group.getId());
        member.setJoinedAt(now);
        member.setRole("MEMBER");
        member.setUserId(userId);

        int insertedRows = taskGroupMemberMapper.insert(member);

        if(insertedRows != 1){
            throw new IllegalStateException("加入工作组失败");
        }

        GroupResponse response = taskGroupMapper.selectGroupResponseByIdAndMemberUserId(group.getId(), userId);

        if(response == null){
            throw new IllegalStateException("读取加入后的工作组失败");
        }

        return response;
    }

    public void rejectInvitation(Long userId, Long invitationId){
        int updatedRows = taskGroupInvitationMapper.rejectInvitation(invitationId, userId);

        if(updatedRows != 1){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "待处理邀请不存在");
        }
    }

    private User findInviteeByAccount(String account){
        String normalizedAccount = account.trim();

        User invitee = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, normalizedAccount)
                    .or()
                    .eq(User::getEmail, normalizedAccount)
        );

        if(invitee == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "被邀请人不存在");
        }

        return invitee;
    }

    private void ensureOwner(TaskGroup group, Long inviterId){
        if(!inviterId.equals(group.getOwnerId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "你不是当前工作组的组长");
        }
    }

    private void ensureNotMember(Long groupId, Long inviteeId){
        TaskGroupMember inviteeMember = taskGroupMemberMapper.selectOne(
            new LambdaQueryWrapper<TaskGroupMember>()
                    .eq(TaskGroupMember::getGroupId, groupId)
                    .eq(TaskGroupMember::getUserId, inviteeId)
        );

        if(inviteeMember != null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "被邀请人已经在当前工作组内");
        }
    }

    private void ensureNoPendingInvitation(Long groupId, Long inviteeId){
        Long pendingCount = taskGroupInvitationMapper.selectCount(
            new LambdaQueryWrapper<TaskGroupInvitation>()
                    .eq(TaskGroupInvitation::getGroupId, groupId)
                    .eq(TaskGroupInvitation::getInviteeId, inviteeId)
                    .eq(TaskGroupInvitation::getStatus, "PENDING")
        );

        if(pendingCount > 0){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已向该成员发送过邀请");
        }
    }

    private InvitationResponse toResponse(
        TaskGroupInvitation invitation,
        TaskGroup group,
        User inviter,
        User invitee
    ){
        InvitationResponse response = new InvitationResponse();

        response.setId(invitation.getId());
        response.setGroupId(invitation.getGroupId());
        response.setGroupName(group.getName());
        response.setInviterId(invitation.getInviterId());
        response.setInviterName(inviter.getUsername());
        response.setInviteeId(invitation.getInviteeId());
        response.setInviteeName(invitee.getUsername());
        response.setStatus(invitation.getStatus());
        response.setCreatedAt(invitation.getCreatedAt());
        response.setHandledAt(invitation.getHandledAt());

        return response;
    }

    private void ensureOwnerOrAdmin(TaskGroup group, Long operatorId){
        TaskGroupMember membership = taskGroupMemberMapper.selectOne(
            new LambdaQueryWrapper<TaskGroupMember>()
                    .eq(TaskGroupMember::getGroupId, group.getId())
                    .eq(TaskGroupMember::getUserId, operatorId)
        );

        if(membership == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "成员关系不存在");
        }

        if(!"ADMIN".equals(membership.getRole()) && !"OWNER".equals(membership.getRole())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员或负责人可以进行该操作");
        }
    }
}
