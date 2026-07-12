package com.shou.lims.organize.role.converter;

import com.shou.lims.organize.role.dto.RoleCreateDTO;
import com.shou.lims.organize.role.entity.Role;
import com.shou.lims.organize.role.vo.RoleVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleConverter {
    RoleVO toVO(Role entity);
    Role toEntity(RoleCreateDTO dto);
    List<RoleVO> toVOList(List<Role> entityList);
}
