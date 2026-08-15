package com.qijx.aitodo.group.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qijx.aitodo.group.dto.GroupTaskCreateRequest;
import com.qijx.aitodo.group.dto.GroupTaskPageResponse;
import com.qijx.aitodo.group.dto.GroupTaskResponse;
import com.qijx.aitodo.group.entity.GroupTask;
import com.qijx.aitodo.group.entity.TaskGroup;
import com.qijx.aitodo.group.entity.TaskGroupMember;
import com.qijx.aitodo.group.mapper.GroupTaskMapper;
import com.qijx.aitodo.group.mapper.TaskGroupMapper;
import com.qijx.aitodo.group.mapper.TaskGroupMemberMapper;
import com.qijx.aitodo.user.entity.User;
import com.qijx.aitodo.user.mapper.UserMapper;

@Service
public class GroupTaskService {
    private final GroupTaskMapper groupTaskMapper;
    private final TaskGroupMapper taskGroupMapper;
    private final TaskGroupMemberMapper taskGroupMemberMapper;
    private final UserMapper userMapper;

    public GroupTaskService(GroupTaskMapper groupTaskMapper, TaskGroupMapper taskGroupMapper, TaskGroupMemberMapper taskGroupMemberMapper, UserMapper userMapper){
        this.groupTaskMapper = groupTaskMapper;
        this.taskGroupMapper = taskGroupMapper;
        this.taskGroupMemberMapper = taskGroupMemberMapper;
        this.userMapper = userMapper;
    }

    public GroupTaskResponse createGroupTask(Long creatorId, Long groupId, GroupTaskCreateRequest request){
        TaskGroup group = taskGroupMapper.selectById(groupId);

        if(group == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该工作组不存在");
        }

        TaskGroupMember creatorMembership = taskGroupMemberMapper.selectOne(
            new LambdaQueryWrapper<TaskGroupMember>()
                    .eq(TaskGroupMember::getGroupId, groupId)
                    .eq(TaskGroupMember::getUserId, creatorId)
        );

        if(creatorMembership == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "创建者不在当前组内");
        }

        if(!"OWNER".equals(creatorMembership.getRole()) && !"ADMIN".equals(creatorMembership.getRole())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有负责人和管理员可以创建任务");
        }

        if(request.getAssigneeId() != null){
            TaskGroupMember assigneeMembership = taskGroupMemberMapper.selectOne(
            new LambdaQueryWrapper<TaskGroupMember>()
                    .eq(TaskGroupMember::getGroupId, groupId)
                    .eq(TaskGroupMember::getUserId, request.getAssigneeId())
            );

            if(assigneeMembership == null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "负责人不属于该小组");
            }
        }

        GroupTask task = new GroupTask();

        LocalDateTime now = LocalDateTime.now();

        task.setGroupId(groupId);
        task.setCreatorId(creatorId);
        task.setAssigneeId(request.getAssigneeId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus("TODO");
        task.setPriority(resolvePriority(request.getPriority()));
        task.setDueAt(request.getDueAt());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        int insertedRows = groupTaskMapper.insert(task);

        if(insertedRows != 1){
            throw new IllegalStateException("添加团队任务失败");
        }

        return toResponse(task);
    }

    public GroupTaskPageResponse listGroupTasks(Long userId, Long groupId, Long page, Long size){        
        if(page == null || page < 1){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "查询页不能小于1");
        }

        if(size == null || size < 1 || size > 50){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "查询页大小不能小于1或者大于50");
        }

        ensureGroupMember(userId, groupId);

        Page<GroupTaskResponse> pageRequest = new Page<>(page, size);

        Page<GroupTaskResponse> taskPage = groupTaskMapper.selectGroupTaskPage(pageRequest, groupId);

        GroupTaskPageResponse response = new GroupTaskPageResponse();

        response.setRecords(taskPage.getRecords());
        response.setPage(taskPage.getCurrent());
        response.setSize(taskPage.getSize());
        response.setTotal(taskPage.getTotal());
        response.setPages(taskPage.getPages());

        return response;
    }

    public GroupTaskResponse getGroupTaskDetail(Long userId, Long groupId, Long taskId){
        ensureGroupMember(userId, groupId);

        GroupTaskResponse response = groupTaskMapper.selectGroupTaskDetail(groupId, taskId);

        if(response == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        return response;
    }

    private String resolvePriority(String priority){
        if(priority == null || priority.isBlank()){
            return "MEDIUM";
        }

        String normalizedPriority = priority.trim();

        if("LOW".equals(normalizedPriority) || "MEDIUM".equals(normalizedPriority) || "HIGH".equals(normalizedPriority)){
            return normalizedPriority;
        }else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务优先级不正确");
        }
    }

    private void ensureGroupMember(Long userId, Long groupId){
        TaskGroupMember membership = taskGroupMemberMapper.selectOne(
            new LambdaQueryWrapper<TaskGroupMember>()
                    .eq(TaskGroupMember::getUserId, userId)
                    .eq(TaskGroupMember::getGroupId, groupId)
        );

        if(membership == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该用户不属于当前小组");
        }
    }

    private GroupTaskResponse toResponse(GroupTask task){
        User creator = userMapper.selectById(task.getCreatorId());

        if(creator == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务创建者不存在");
        }

        GroupTaskResponse response = new GroupTaskResponse();

        response.setId(task.getId());
        response.setGroupId(task.getGroupId());
        response.setCreatorId(task.getCreatorId());
        response.setCreatorName(creator.getUsername());
        response.setAssigneeId(task.getAssigneeId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDueAt(task.getDueAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        if(task.getAssigneeId() != null){
            User assignee = userMapper.selectById(task.getAssigneeId());

            if(assignee == null){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务负责人不存在");
            }

            response.setAssigneeName(assignee.getUsername());
        }

        return response;
    }
}
