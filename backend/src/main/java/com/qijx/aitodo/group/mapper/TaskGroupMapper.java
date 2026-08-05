package com.qijx.aitodo.group.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qijx.aitodo.group.dto.GroupResponse;
import com.qijx.aitodo.group.entity.TaskGroup;

@Mapper
public interface TaskGroupMapper extends BaseMapper<TaskGroup>{
    @Select("""
            SELECT
                g.id,
                g.name,
                g.description,
                g.owner_id,
                m.role AS current_user_role,
                g.created_at,
                g.updated_at
            FROM task_groups g
            INNER JOIN task_group_members m
                ON m.group_id = g.id
            WHERE m.user_id = #{userId}
            ORDER BY g.created_at DESC
            """)
    List<GroupResponse> selectGroupsByMemberUserId(
        @Param("userId") Long userId
    );
}
