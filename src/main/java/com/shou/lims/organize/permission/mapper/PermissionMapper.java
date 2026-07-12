package com.shou.lims.organize.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shou.lims.organize.permission.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("SELECT DISTINCT p.* FROM sys_permission p INNER JOIN sys_role_permission rp ON p.id = rp.permission_id INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id WHERE ur.user_id = #{userId} AND p.is_delete = 0")
    List<Permission> selectByUserId(Long userId);
}
