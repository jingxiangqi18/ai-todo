package com.qijx.aitodo.group.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qijx.aitodo.group.dto.InvitationResponse;
import com.qijx.aitodo.group.entity.TaskGroupInvitation;

@Mapper
public interface TaskGroupInvitationMapper extends BaseMapper<TaskGroupInvitation>{
    @Select("""
            SELECT
              i.id,
              i.group_id,
              g.name AS group_name,

              i.inviter_id,
              inviter.username AS inviter_name,

              i.invitee_id,
              invitee.username AS invitee_name,

              i.status,
              i.created_at,
              i.handled_at

            FROM task_group_invitations AS i

            INNER JOIN task_groups AS g
                ON g.id = i.group_id

            INNER JOIN users AS inviter
                ON inviter.id = i.inviter_id

            INNER JOIN users invitee
                ON invitee.id = i.invitee_id

            WHERE i.invitee_id = #{inviteeId}
                AND i.status = 'PENDING'

            ORDER BY i.created_at DESC
            """)
    List<InvitationResponse> selectPendingInvitationResponse(
        @Param("inviteeId") Long inviteeId
    );

    @Update("""
            UPDATE task_group_invitations
            SET
                status = 'ACCEPTED',
                handled_at = CURRENT_TIMESTAMP
            WHERE id = #{invitationId}
                AND invitee_id = #{inviteeId}
                AND status = 'PENDING'
    """)
    int acceptInvitation(
        @Param("invitationId") Long invitationId,
        @Param("inviteeId") Long inviteeId
    );

    @Update("""
            UPDATE task_group_invitations
            SET
                status = 'REJECTED',
                handled_at = CURRENT_TIMESTAMP
            WHERE id = #{invitationId}
                AND invitee_id = #{inviteeId}
                AND status = 'PENDING'
            """)
    int rejectInvitation(
        @Param("invitationId") Long invitationId,
        @Param("inviteeId") Long inviteeId
    );
}
