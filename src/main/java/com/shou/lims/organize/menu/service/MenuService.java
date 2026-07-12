package com.shou.lims.organize.menu.service;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.menu.dto.MenuCreateDTO;
import com.shou.lims.organize.menu.dto.MenuQueryDTO;
import com.shou.lims.organize.menu.dto.MenuUpdateDTO;
import com.shou.lims.organize.menu.vo.MenuVO;
import java.util.List;

public interface MenuService {
    PageVO<MenuVO> page(MenuQueryDTO query);
    MenuVO getById(Long id);
    Long create(MenuCreateDTO dto);
    void update(Long id, MenuUpdateDTO dto);
    void delete(List<Long> ids);
    List<MenuVO> getTree();
}
