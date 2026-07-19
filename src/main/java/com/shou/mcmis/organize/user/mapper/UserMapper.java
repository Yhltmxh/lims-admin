package com.shou.mcmis.organize.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shou.mcmis.organize.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @org.apache.ibatis.annotations.Update("UPDATE sys_user SET auth_version = auth_version + 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND is_delete = 0")
    int incrementAuthVersion(Long id);
}
