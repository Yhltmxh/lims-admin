package com.shou.mcmis.organize.role.converter;

import com.shou.mcmis.organize.role.dto.RoleCreateDTO;
import com.shou.mcmis.organize.role.entity.Role;
import com.shou.mcmis.organize.role.vo.RoleVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleConverter {
    RoleVO toVO(Role entity);
    Role toEntity(RoleCreateDTO dto);
    List<RoleVO> toVOList(List<Role> entityList);
}
