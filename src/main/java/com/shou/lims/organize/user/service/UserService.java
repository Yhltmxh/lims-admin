package com.shou.lims.organize.user.service;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.user.dto.UserCreateDTO;
import com.shou.lims.organize.user.dto.UserQueryDTO;
import com.shou.lims.organize.user.dto.UserUpdateDTO;
import com.shou.lims.organize.user.vo.UserVO;
import java.util.List;

public interface UserService {
    PageVO<UserVO> page(UserQueryDTO query);
    UserVO getById(Long id);
    Long create(UserCreateDTO dto);
    void update(Long id, UserUpdateDTO dto);
    void delete(List<Long> ids);
}
