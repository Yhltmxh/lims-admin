package com.shou.lims.organize.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shou.lims.organize.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.* FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} AND r.is_delete = 0")
    List<Role> selectByUserId(Long userId);
}
