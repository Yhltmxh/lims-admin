package com.shou.lims.organize.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shou.lims.organize.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Collection;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.* FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND r.is_delete = 0 AND r.status = 1")
    List<Role> selectByUserId(Long userId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteRolePermissions(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenus(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    void insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Select("SELECT rp.permission_id FROM sys_role_permission rp "
            + "INNER JOIN sys_permission p ON p.id = rp.permission_id "
            + "WHERE rp.role_id = #{roleId} AND p.is_delete = 0 ORDER BY rp.permission_id")
    List<Long> selectRolePermissionIds(@Param("roleId") Long roleId);

    @Select("SELECT DISTINCT role_id FROM sys_role_permission WHERE permission_id = #{permissionId}")
    List<Long> selectRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    @Select("SELECT rm.menu_id FROM sys_role_menu rm "
            + "INNER JOIN sys_menu m ON m.id = rm.menu_id "
            + "WHERE rm.role_id = #{roleId} AND m.is_delete = 0 ORDER BY rm.menu_id")
    List<Long> selectRoleMenuIds(@Param("roleId") Long roleId);

    @Delete("""
            <script>
            DELETE FROM sys_role_permission WHERE permission_id IN
            <foreach collection="permissionIds" item="permissionId" open="(" separator="," close=")">
                #{permissionId}
            </foreach>
            </script>
            """)
    int deleteRolePermissionsByPermissionIds(@Param("permissionIds") Collection<Long> permissionIds);

    @Delete("""
            <script>
            DELETE FROM sys_role_menu WHERE menu_id IN
            <foreach collection="menuIds" item="menuId" open="(" separator="," close=")">
                #{menuId}
            </foreach>
            </script>
            """)
    int deleteRoleMenusByMenuIds(@Param("menuIds") Collection<Long> menuIds);
}
