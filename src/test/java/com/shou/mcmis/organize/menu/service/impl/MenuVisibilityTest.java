package com.shou.mcmis.organize.menu.service.impl;

import com.shou.mcmis.organize.menu.converter.MenuConverter;
import com.shou.mcmis.organize.menu.entity.Menu;
import com.shou.mcmis.organize.menu.mapper.MenuMapper;
import com.shou.mcmis.organize.menu.vo.MenuRouteVO;
import com.shou.mcmis.organize.permission.entity.Permission;
import com.shou.mcmis.organize.permission.mapper.PermissionMapper;
import com.shou.mcmis.organize.role.mapper.RoleMapper;
import com.shou.mcmis.security.service.EffectivePermissionService;
import com.shou.mcmis.security.service.EffectivePermissionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuVisibilityTest {

    @Mock
    private MenuMapper menuMapper;
    @Mock
    private MenuConverter menuConverter;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private EffectivePermissionService effectivePermissionService;
    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    void shouldShowUnrestrictedLeafWithoutShowingUnauthorizedDirectory() {
        Menu welcome = menu(1L, 0L, "欢迎", "/welcome", null);
        Menu system = menu(2L, 0L, "系统管理", "/system", null);
        Menu users = menu(3L, 2L, "用户管理", "/system/users", 10L);
        Permission userList = new Permission();
        userList.setId(10L);
        userList.setCode("organize:user:list");

        when(menuMapper.selectList(any())).thenReturn(List.of(welcome, system, users));
        when(permissionMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(userList));
        when(effectivePermissionService.resolve(7L)).thenReturn(new EffectivePermissionSnapshot());

        List<MenuRouteVO> routes = menuService.getCurrentUserMenuTree(7L);

        assertThat(routes).extracting(MenuRouteVO::getPath).containsExactly("/welcome");
    }

    private Menu menu(Long id, Long parentId, String name, String path, Long permissionId) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setPath(path);
        menu.setRequiredPermissionId(permissionId);
        return menu;
    }
}
