package com.shou.lims.organize.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shou.lims.organize.menu.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
            INNER JOIN sys_role r ON r.id = rm.role_id
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.is_delete = 0 AND r.status = 1
              AND m.is_delete = 0 AND m.status = 1
            ORDER BY m.sort_order
            """)
    List<Menu> selectByUserId(Long userId);
}
