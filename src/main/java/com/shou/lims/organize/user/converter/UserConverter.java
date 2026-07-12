package com.shou.lims.organize.user.converter;

import com.shou.lims.organize.user.dto.UserCreateDTO;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.vo.UserVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserConverter {
    UserVO toVO(User entity);
    User toEntity(UserCreateDTO dto);
    List<UserVO> toVOList(List<User> entityList);
}
