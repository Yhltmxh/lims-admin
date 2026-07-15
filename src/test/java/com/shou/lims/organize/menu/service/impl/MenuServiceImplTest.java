package com.shou.lims.organize.menu.service.impl;

import com.shou.lims.BaseSpringBootTest;
import com.shou.lims.organize.menu.service.MenuService;
import com.shou.lims.organize.menu.vo.MenuVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MenuServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private MenuService menuService;

    /**
     * 种子数据结构说明：init.sql 的菜单插入语句
     * {@code INSERT INTO sys_menu (name, path, component, icon, ...)} 未指定 parent_id，
     * 列默认值为 0，因此 3 个菜单（系统管理/用户管理/角色管理）全部是根节点，
     * 树中不存在嵌套 children。这里只断言根节点列表本身，不断言 children 非空。
     */
    @Test
    void shouldGetTree() {
        List<MenuVO> tree = menuService.getTree();
        assertThat(tree).isNotEmpty();
        assertThat(tree).extracting(MenuVO::getName).contains("系统管理", "用户管理", "角色管理");
    }
}
