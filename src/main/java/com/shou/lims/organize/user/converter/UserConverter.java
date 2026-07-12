package com.shou.lims.organize.user.converter;

import com.shou.lims.organize.user.dto.UserCreateDTO;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.vo.UserVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserConverter {
    UserVO toVO(User entity);
    User toEntity(UserCreateDTO dto);
    java.util.List<UserVO> toVOList(java.util.List<User> entityList);
}
