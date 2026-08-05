package com.qijx.aitodo.group.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qijx.aitodo.group.dto.GroupCreateRequest;
import com.qijx.aitodo.group.dto.GroupMemberResponse;
import com.qijx.aitodo.group.dto.GroupResponse;
import com.qijx.aitodo.group.entity.TaskGroup;
import com.qijx.aitodo.group.entity.TaskGroupMember;
import com.qijx.aitodo.group.mapper.TaskGroupMapper;
import com.qijx.aitodo.group.mapper.TaskGroupMemberMapper;

@Service
public class GroupService {
    private final TaskGroupMapper taskGroupMapper;
    private final TaskGroupMemberMapper taskGroupMemberMapper;

    public GroupService(TaskGroupMapper taskGroupMapper, TaskGroupMemberMapper taskGroupMemberMapper){
        this.taskGroupMapper = taskGroupMapper;
        this.taskGroupMemberMapper = taskGroupMemberMapper;
    }

    @Transactional
    public GroupResponse createGroup(Long userId, GroupCreateRequest request){
        String groupName = request.getName().trim();

        Long duplicateCount = taskGroupMapper.selectCount(
            new LambdaQueryWrapper<TaskGroup>()
                .eq(TaskGroup::getOwnerId, userId)
                .eq(TaskGroup::getName, groupName)    
        );

        if(duplicateCount > 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "你已创建过同名工作组");
        }

        LocalDateTime now = LocalDateTime.now();

        TaskGroup taskGroup = new TaskGroup();

        taskGroup.setName(groupName);
        taskGroup.setDescription(request.getDescription());
        taskGroup.setOwnerId(userId);
        taskGroup.setCreatedAt(now);
        taskGroup.setUpdatedAt(now);

        int insertedGroups = taskGroupMapper.insert(taskGroup);

        if(insertedGroups != 1){
            throw new IllegalStateException("创建小组失败");
        }

        TaskGroupMember ownerMember = new TaskGroupMember();

        ownerMember.setGroupId(taskGroup.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole("OWNER");
        ownerMember.setJoinedAt(now);

        int insertedGroupMembers = taskGroupMemberMapper.insert(ownerMember);

        if(insertedGroupMembers != 1){
            throw new IllegalStateException("创建小组负责人失败");
        }

        return toResponse(taskGroup, "OWNER");
    }

    public List<GroupResponse> listMyGroups(Long userId){
        return taskGroupMapper.selectGroupsByMemberUserId(userId);
    }

    public GroupResponse getGroupDetail(Long userId, Long groupId){
        TaskGroupMember membership = findMembership(userId, groupId);

        TaskGroup group = taskGroupMapper.selectById(groupId);

        if(group == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "小组不存在");
        }

        return toResponse(group, membership.getRole());
    }

    public List<GroupMemberResponse> listGroupMembers(Long userId, Long groupId){
        findMembership(userId, groupId);

        return taskGroupMemberMapper.selectGroupMembers(groupId);
    }

    private TaskGroupMember findMembership(Long userId, Long groupId){
        TaskGroupMember membership = taskGroupMemberMapper.selectOne(
            new LambdaQueryWrapper<TaskGroupMember>()
                    .eq(TaskGroupMember::getGroupId, groupId)
                    .eq(TaskGroupMember::getUserId, userId)
        );

        if(membership == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "小组不存在或你不是小组成员");
        }

        return membership;
    }

    private GroupResponse toResponse(TaskGroup group, String role){
        GroupResponse response = new GroupResponse();

        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setOwnerId(group.getOwnerId());
        response.setCurrentUserRole(role);
        response.setCreatedAt(group.getCreatedAt());
        response.setUpdatedAt(group.getUpdatedAt());

        return response;
    }
}
