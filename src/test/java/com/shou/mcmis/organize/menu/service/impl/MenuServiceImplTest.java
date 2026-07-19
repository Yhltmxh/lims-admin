package com.shou.mcmis.organize.menu.service.impl;

import com.shou.mcmis.BaseSpringBootTest;
import com.shou.mcmis.organize.menu.service.MenuService;
import com.shou.mcmis.organize.menu.dto.MenuUpdateDTO;
import com.shou.mcmis.organize.menu.vo.MenuVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MenuServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private MenuService menuService;

    @Test
    void shouldGetTree() {
        List<MenuVO> tree = menuService.getTree();
        assertThat(tree).isNotEmpty();
        MenuVO system = tree.stream().filter(menu -> "系统管理".equals(menu.getName()))
                .findFirst().orElseThrow();
        assertThat(system.getChildren()).extracting(MenuVO::getName)
                .contains("用户管理", "角色管理");
    }

    @Test
    void shouldClearRequiredPermissionWithZeroSentinel() {
        MenuVO menu = menuService.getTree().stream()
                .flatMap(root -> root.getChildren().stream())
                .filter(item -> item.getRequiredPermissionId() != null)
                .findFirst().orElseThrow();
        MenuUpdateDTO dto = new MenuUpdateDTO();
        dto.setRequiredPermissionId(0L);
        dto.setVersion(menu.getVersion());

        menuService.update(menu.getId(), dto);

        assertThat(menuService.getById(menu.getId()).getRequiredPermissionId()).isNull();
    }
}
