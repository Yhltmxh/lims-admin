package com.shou.mcmis.organize.permission.converter;

import com.shou.mcmis.organize.permission.dto.PermissionCreateDTO;
import com.shou.mcmis.organize.permission.entity.Permission;
import com.shou.mcmis.organize.permission.vo.PermissionVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissionConverter {
    PermissionVO toVO(Permission entity);
    Permission toEntity(PermissionCreateDTO dto);
    List<PermissionVO> toVOList(List<Permission> entityList);
}
