package com.qijx.aitodo.group.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qijx.aitodo.group.dto.GroupTaskResponse;
import com.qijx.aitodo.group.entity.GroupTask;

@Mapper
public interface GroupTaskMapper extends BaseMapper<GroupTask>{
    @Select("""
            SELECT
                task.id,
                task.group_id AS groupId,
                task.creator_id AS creatorId,
                creator.username AS creatorName,
                task.assignee_id AS assigneeId,
                assignee.username AS assigneeName,
                task.title,
                task.description,
                task.status,
                task.priority,
                task.due_at AS dueAt,
                task.created_at AS createdAt,
                task.updated_at AS updatedAt
            FROM group_tasks task
            INNER JOIN users creator
                ON creator.id = task.creator_id
            LEFT JOIN users assignee
                ON assignee.id = task.assignee_id
            WHERE task.group_id = #{groupId}
            ORDER BY task.created_at DESC
            """)
    Page<GroupTaskResponse> selectGroupTaskPage(
        Page<GroupTaskResponse> page,
        @Param("groupId") Long groupId
    );

    @Select("""
            SELECT
                task.id,
                task.group_id AS groupId,
                task.creator_id As creatorId,
                creator.username AS creatorName,
                task.assignee_id AS assigneeId,
                assignee.username AS assigneeName,
                task.title,
                task.description,
                task.status,
                task.priority,
                task.due_at AS dueAt,
                task.created_at AS createdAt,
                task.updated_at AS updatedAt
            FROM group_tasks task
            INNER JOIN users creator
                ON creator.id = task.creator_id
            LEFT JOIN users assignee
                ON assignee.id = task.assignee_id
            WHERE task.group_id = #{groupId}
                AND task.id = #{taskId}
            """)
    GroupTaskResponse selectGroupTaskDetail(
        @Param("groupId") Long groupId,
        @Param("taskId") Long taskId
    );
}
