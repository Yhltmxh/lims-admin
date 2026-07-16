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
