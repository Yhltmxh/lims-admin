package com.shou.mcmis.organize.menu.service;

import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.menu.dto.MenuCreateDTO;
import com.shou.mcmis.organize.menu.dto.MenuQueryDTO;
import com.shou.mcmis.organize.menu.dto.MenuUpdateDTO;
import com.shou.mcmis.organize.menu.vo.MenuVO;
import com.shou.mcmis.organize.menu.vo.MenuRouteVO;
import com.shou.mcmis.organize.menu.vo.MenuPermissionOptionVO;
import java.util.List;

public interface MenuService {
    PageVO<MenuVO> page(MenuQueryDTO query);
    MenuVO getById(Long id);
    Long create(MenuCreateDTO dto);
    void update(Long id, MenuUpdateDTO dto);
    void delete(List<Long> ids);
    List<MenuVO> getTree();
    List<MenuRouteVO> getCurrentUserMenuTree(Long userId);
    List<MenuPermissionOptionVO> permissionOptions();
}
