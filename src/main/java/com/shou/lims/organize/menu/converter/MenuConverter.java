package com.shou.lims.organize.menu.converter;

import com.shou.lims.organize.menu.dto.MenuCreateDTO;
import com.shou.lims.organize.menu.entity.Menu;
import com.shou.lims.organize.menu.vo.MenuVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MenuConverter {
    MenuVO toVO(Menu entity);
    Menu toEntity(MenuCreateDTO dto);
    List<MenuVO> toVOList(List<Menu> entityList);
}
