package com.shou.mcmis.organize.userpermission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shou.mcmis.organize.userpermission.entity.UserPermissionAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface UserPermissionAuditMapper extends BaseMapper<UserPermissionAudit> {
    @Insert("""
            INSERT INTO sys_user_permission_audit
            (grant_id, user_id, permission_id, operation, before_data, after_data, reason,
             operator_id, operator_username, request_id, ip, create_time)
            VALUES (#{grantId}, #{userId}, #{permissionId}, #{operation},
                    CAST(#{beforeData} AS jsonb), CAST(#{afterData} AS jsonb), #{reason},
                    #{operatorId}, #{operatorUsername}, #{requestId}, #{ip}, CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAudit(UserPermissionAudit audit);

    @Select("""
            SELECT * FROM sys_user_permission_audit
            WHERE user_id = #{userId}
            ORDER BY create_time DESC, id DESC
            LIMIT 500
            """)
    List<UserPermissionAudit> selectByUserId(Long userId);
}
