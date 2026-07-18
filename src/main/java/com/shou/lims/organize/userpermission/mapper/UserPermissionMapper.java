package com.shou.lims.organize.userpermission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shou.lims.organize.userpermission.entity.UserPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {
    @Select("""
            SELECT * FROM sys_user_permission
            WHERE user_id = #{userId} AND is_delete = 0
            ORDER BY permission_id, valid_from NULLS FIRST, id
            """)
    List<UserPermission> selectAllByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*) FROM sys_user_permission
            WHERE user_id = #{userId} AND permission_id = #{permissionId} AND is_delete = 0
              AND (CAST(#{excludeId} AS BIGINT) IS NULL OR id != CAST(#{excludeId} AS BIGINT))
              AND tsrange(COALESCE(valid_from, '-infinity'::timestamp),
                          COALESCE(expires_at, 'infinity'::timestamp), '[)')
                  && tsrange(COALESCE(CAST(#{validFrom} AS TIMESTAMP), '-infinity'::timestamp),
                             COALESCE(CAST(#{expiresAt} AS TIMESTAMP), 'infinity'::timestamp), '[)')
            """)
    long countOverlaps(@Param("userId") Long userId,
                       @Param("permissionId") Long permissionId,
                       @Param("validFrom") LocalDateTime validFrom,
                       @Param("expiresAt") LocalDateTime expiresAt,
                       @Param("excludeId") Long excludeId);

    @Select("""
            SELECT DISTINCT user_id FROM sys_user_permission
            WHERE permission_id = #{permissionId} AND is_delete = 0
            """)
    List<Long> selectUserIdsByPermissionId(@Param("permissionId") Long permissionId);
}
