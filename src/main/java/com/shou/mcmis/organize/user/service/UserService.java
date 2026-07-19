package com.shou.mcmis.organize.user.service;

import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.user.dto.UserCreateDTO;
import com.shou.mcmis.organize.user.dto.UserQueryDTO;
import com.shou.mcmis.organize.user.dto.UserUpdateDTO;
import com.shou.mcmis.organize.user.vo.UserVO;
import java.util.List;

public interface UserService {
    PageVO<UserVO> page(UserQueryDTO query);
    UserVO getById(Long id);
    Long create(UserCreateDTO dto);
    void update(Long id, UserUpdateDTO dto);
    void delete(List<Long> ids);
}
