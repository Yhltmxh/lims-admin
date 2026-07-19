package com.shou.mcmis.organize.role.service.impl;

import com.shou.mcmis.BaseSpringBootTest;
import com.shou.mcmis.common.exception.BusinessException;
import com.shou.mcmis.common.exception.NotFoundException;
import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.role.dto.RoleCreateDTO;
import com.shou.mcmis.organize.role.dto.RoleQueryDTO;
import com.shou.mcmis.organize.role.dto.RoleUpdateDTO;
import com.shou.mcmis.organize.role.mapper.RoleMapper;
import com.shou.mcmis.organize.role.service.RoleService;
import com.shou.mcmis.organize.role.vo.RoleVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RoleServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMapper roleMapper;

    @Test
    void shouldPageRoles() {
        RoleQueryDTO query = new RoleQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        PageVO<RoleVO> result = roleService.page(query);
        assertThat(result.getTotal()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void shouldGetById() {
        RoleVO role = roleService.getById(1L);
        assertThat(role.getName()).isEqualTo("ROLE_ADMIN");
        assertThat(role.getPermissionIds()).isNotEmpty();
        assertThat(role.getMenuIds()).isNotEmpty();
    }

    @Test
    void shouldThrowNotFoundForMissingRole() {
        assertThatThrownBy(() -> roleService.getById(9999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldCreateRole() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setName("ROLE_TEST");
        dto.setLabel("测试角色");
        Long id = roleService.create(dto);
        assertThat(id).isNotNull();
        assertThat(roleService.getById(id).getName()).isEqualTo("ROLE_TEST");
    }

    @Test
    void shouldRejectDuplicateRoleName() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setName("ROLE_ADMIN");
        dto.setLabel("dup");
        assertThatThrownBy(() -> roleService.create(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(409);
    }

    @Test
    void shouldUpdateRole() {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setLabel("新标签");
        roleService.update(2L, dto);
        assertThat(roleService.getById(2L).getLabel()).isEqualTo("新标签");
    }

    @Test
    void shouldDeleteRole() {
        roleService.delete(List.of(2L));
        assertThatThrownBy(() -> roleService.getById(2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldAssignPermissions() {
        roleService.assignPermissions(2L, List.of(3L, 4L));
        assertThat(roleMapper.selectRolePermissionIds(2L)).contains(3L, 4L);
    }

    @Test
    void shouldReplacePermissionsOnReassign() {
        roleService.assignPermissions(2L, List.of(3L, 4L));
        roleService.assignPermissions(2L, List.of(5L));
        assertThat(roleMapper.selectRolePermissionIds(2L)).contains(5L).doesNotContain(3L, 4L);
    }

    @Test
    void shouldAssignMenus() {
        roleService.assignMenus(2L, List.of(1L, 2L));
        assertThat(roleMapper.selectRoleMenuIds(2L)).contains(1L, 2L);
    }
}
