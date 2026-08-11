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
import com.qijx.aitodo.group.dto.GroupMemberRoleUpdateRequest;
import com.qijx.aitodo.group.dto.GroupResponse;
import com.qijx.aitodo.group.dto.InvitationCreateRequest;
import com.qijx.aitodo.group.dto.InvitationResponse;
import com.qijx.aitodo.group.entity.TaskGroup;
import com.qijx.aitodo.group.entity.TaskGroupMember;
import com.qijx.aitodo.group.mapper.TaskGroupMapper;
import com.qijx.aitodo.group.mapper.TaskGroupMemberMapper;
import com.qijx.aitodo.user.mapper.UserMapper;

@Service
public class GroupService {
    private final TaskGroupMapper taskGroupMapper;
    private final TaskGroupMemberMapper taskGroupMemberMapper;
    private final UserMapper userMapper;

    public GroupService(TaskGroupMapper taskGroupMapper, TaskGroupMemberMapper taskGroupMemberMapper, UserMapper userMapper){
        this.taskGroupMapper = taskGroupMapper;
        this.taskGroupMemberMapper = taskGroupMemberMapper;
        this.userMapper = userMapper;
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

        return toGroupResponse(taskGroup, "OWNER");
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

        return toGroupResponse(group, membership.getRole());
    }

    public List<GroupMemberResponse> listGroupMembers(Long userId, Long groupId){
        findMembership(userId, groupId);

        return taskGroupMemberMapper.selectGroupMembers(groupId);
    }

    public void leaveGroup(Long userId, Long groupId){
        TaskGroup group = taskGroupMapper.selectById(groupId);

        if(group == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该工作组不存在");
        }

        TaskGroupMember membership = findMembership(userId, groupId);

        if("OWNER".equals(membership.getRole()) || userId.equals(group.getOwnerId())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "小组负责人不能直接退出，请先转让负责人或解散小组");
        }

        int deleteRows = taskGroupMemberMapper.deleteById(membership.getId());

        if(deleteRows != 1){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "退出失败，成员关系已发生变化");
        }
    }

    public GroupMemberResponse updateMemberRole(Long operatorId, Long groupId, Long memberUserId, GroupMemberRoleUpdateRequest request){
        String role = request.getRole();

        //验证是否为ADMIN或MEMBER
        String normalizedRole = resolveAssignableRole(role);

        //确认组存在
        TaskGroup group = findGroup(groupId);

        //确认关系，这一步是找操作者的关系
        TaskGroupMember membership = findMembership(operatorId, groupId);

        //确认找到的关系里操作者是OWNER
        ensureOwner(group, membership);

        //这一步才找被修改role的成员关系
        TaskGroupMember targetMembership = findMembership(memberUserId, groupId);

        //不允许OWNER被修改
        if(memberUserId.equals(group.getOwnerId()) || "OWNER".equals(targetMembership.getRole())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能通过成员角色接口修改小组负责人");
        }

        //如果请求角色不变，直接不执行操作即可
        if(targetMembership.getRole().equals(normalizedRole)){
            return toGroupMemberResponse(targetMembership);
        }

        //正式更新成员角色

        targetMembership.setRole(normalizedRole);
        int updatedRows = taskGroupMemberMapper.updateById(targetMembership);

        if(updatedRows != 1){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "成员角色修改失败，成员关系可能已经变化");
        }

        return toGroupMemberResponse(targetMembership);
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

    private GroupResponse toGroupResponse(TaskGroup group, String role){
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

    private String resolveAssignableRole(String role){
        if(role == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不存在");
        }

        String normalizedRole = role.trim();

        if(!"ADMIN".equals(normalizedRole) && !"MEMBER".equals(normalizedRole)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色身份不正确");            
        }

        return normalizedRole;
    }

    private TaskGroup findGroup(Long groupId){
        TaskGroup group = taskGroupMapper.selectById(groupId);

        if(group == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工作组不存在");
        }

        return group;
    }

    private void ensureOwner(TaskGroup group, TaskGroupMember operatorMembership){
        if(!"OWNER".equals(operatorMembership.getRole()) || !group.getOwnerId().equals(operatorMembership.getUserId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有小组负责人可以修改成员角色");
        }
    }

    private GroupMemberResponse toGroupMemberResponse(TaskGroupMember targetMembership){
        GroupMemberResponse response = new GroupMemberResponse();
        String username = userMapper.selectById(targetMembership.getUserId()).getUsername();

        response.setUserId(targetMembership.getUserId());
        response.setUsername(username);
        response.setRole(targetMembership.getRole());
        response.setJoinedAt(targetMembership.getJoinedAt());

        return response;
    }
}
