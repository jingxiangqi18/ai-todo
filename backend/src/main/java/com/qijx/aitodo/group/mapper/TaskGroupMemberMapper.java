package com.qijx.aitodo.group.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qijx.aitodo.group.dto.GroupMemberResponse;
import com.qijx.aitodo.group.entity.TaskGroupMember;

@Mapper
public interface TaskGroupMemberMapper extends BaseMapper<TaskGroupMember>{
    @Select("""
            SELECT
                m.user_id,
                u.username,
                m.role,
                m.joined_at
            FROM task_group_members m
            INNER JOIN users u
                ON u.id = m.user_id
            WHERE m.group_id = #{groupId}
            ORDER BY m.joined_at ASC
            """)
    List<GroupMemberResponse> selectGroupMembers(
        @Param("groupId") Long groupId
    );
}
