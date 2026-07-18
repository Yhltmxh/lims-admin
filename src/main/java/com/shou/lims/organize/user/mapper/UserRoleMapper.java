package com.shou.lims.organize.user.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserRoleMapper {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("""
            <script>
            DELETE FROM sys_user_role WHERE user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
            </foreach>
            </script>
            """)
    int deleteByUserIds(@Param("userIds") Collection<Long> userIds);

    @Delete("""
            <script>
            DELETE FROM sys_user_role WHERE role_id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
            </foreach>
            </script>
            """)
    int deleteByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} ORDER BY role_id")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT DISTINCT user_id FROM sys_user_role WHERE role_id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
            </foreach>
            </script>
            """)
    List<Long> selectUserIdsByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Select("""
            SELECT COUNT(*) FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE r.name = #{roleName} AND r.status = 1 AND r.is_delete = 0
              AND u.status = 1 AND u.is_delete = 0
              AND (#{excludeUserId} IS NULL OR u.id != #{excludeUserId})
            """)
    long countEnabledUsersByRoleNameExcluding(@Param("roleName") String roleName,
                                               @Param("excludeUserId") Long excludeUserId);

    @Select("""
            <script>
            SELECT ur.user_id AS user_id, r.id AS role_id, r.name AS role_name
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
            </foreach>
              AND r.is_delete = 0
            ORDER BY ur.user_id, r.id
            </script>
            """)
    List<UserRoleRow> selectRoleRowsByUserIds(@Param("userIds") Collection<Long> userIds);
}
