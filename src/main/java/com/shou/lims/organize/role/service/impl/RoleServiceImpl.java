package com.shou.lims.organize.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.common.exception.BusinessException;
import com.shou.lims.common.exception.NotFoundException;
import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.role.converter.RoleConverter;
import com.shou.lims.organize.role.dto.RoleCreateDTO;
import com.shou.lims.organize.role.dto.RoleQueryDTO;
import com.shou.lims.organize.role.dto.RoleUpdateDTO;
import com.shou.lims.organize.role.entity.Role;
import com.shou.lims.organize.role.mapper.RoleMapper;
import com.shou.lims.organize.role.service.RoleService;
import com.shou.lims.organize.role.vo.RoleVO;
import com.shou.lims.organize.permission.entity.Permission;
import com.shou.lims.organize.permission.mapper.PermissionMapper;
import com.shou.lims.organize.menu.entity.Menu;
import com.shou.lims.organize.menu.mapper.MenuMapper;
import com.shou.lims.organize.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleConverter roleConverter;
    private final PermissionMapper permissionMapper;
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public PageVO<RoleVO> page(RoleQueryDTO query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .like(StringUtils.isNotBlank(query.getName()), Role::getName, query.getName())
                .eq(query.getStatus() != null, Role::getStatus, query.getStatus())
                .orderByDesc(Role::getCreateTime);
        List<Role> list = roleMapper.selectList(wrapper);
        PageInfo<Role> pageInfo = new PageInfo<>(list);
        return PageVO.of(pageInfo.convert(roleConverter::toVO));
    }

    @Override
    public RoleVO getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new NotFoundException("角色不存在");
        }
        RoleVO vo = roleConverter.toVO(role);
        fillAssignments(vo);
        return vo;
    }

    @Override
    @Transactional
    public Long create(RoleCreateDTO dto) {
        Role existing = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getName, dto.getName()));
        if (existing != null) {
            throw new BusinessException(409, "角色名称已存在");
        }
        Role role = roleConverter.toEntity(dto);
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusEnum.ENABLED.getValue());
        roleMapper.insert(role);

        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), validatePermissionIds(dto.getPermissionIds()));
        }
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), validateMenuIds(dto.getMenuIds()));
        }
        return role.getId();
    }

    @Override
    @Transactional
    public void update(Long id, RoleUpdateDTO dto) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new NotFoundException("角色不存在");
        }
        if (dto.getVersion() != null && !dto.getVersion().equals(role.getVersion())) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
        if (StringUtils.isNotBlank(dto.getLabel())) role.setLabel(dto.getLabel());
        if (dto.getDescription() != null) role.setDescription(dto.getDescription());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        if (roleMapper.updateById(role) == 0) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }

        if (dto.getPermissionIds() != null) {
            assignPermissions(id, validatePermissionIds(dto.getPermissionIds()));
        }
        if (dto.getMenuIds() != null) {
            assignMenus(id, validateMenuIds(dto.getMenuIds()));
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        userRoleMapper.deleteByRoleIds(ids);
        ids.forEach(id -> {
            roleMapper.deleteRolePermissions(id);
            roleMapper.deleteRoleMenus(id);
        });
        roleMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        requireRole(roleId);
        List<Long> validIds = validatePermissionIds(permissionIds);
        roleMapper.deleteRolePermissions(roleId);
        if (!validIds.isEmpty()) {
            for (Long permissionId : validIds) {
                roleMapper.insertRolePermission(roleId, permissionId);
            }
        }
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        List<Long> validIds = validateMenuIds(menuIds);
        roleMapper.deleteRoleMenus(roleId);
        if (!validIds.isEmpty()) {
            for (Long menuId : validIds) {
                roleMapper.insertRoleMenu(roleId, menuId);
            }
        }
    }

    private Role requireRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new NotFoundException("角色不存在");
        }
        return role;
    }

    private List<Long> validatePermissionIds(Collection<Long> ids) {
        List<Long> distinctIds = distinct(ids);
        if (distinctIds.isEmpty()) {
            return distinctIds;
        }
        Set<Long> existingIds = permissionMapper.selectBatchIds(distinctIds).stream()
                .map(Permission::getId).collect(Collectors.toSet());
        if (existingIds.size() != distinctIds.size() || !existingIds.containsAll(distinctIds)) {
            throw new BusinessException(400, "包含不存在的权限");
        }
        return distinctIds;
    }

    private List<Long> validateMenuIds(Collection<Long> ids) {
        List<Long> distinctIds = distinct(ids);
        if (distinctIds.isEmpty()) {
            return distinctIds;
        }
        Set<Long> existingIds = menuMapper.selectBatchIds(distinctIds).stream()
                .map(Menu::getId).collect(Collectors.toSet());
        if (existingIds.size() != distinctIds.size() || !existingIds.containsAll(distinctIds)) {
            throw new BusinessException(400, "包含不存在的菜单");
        }
        return distinctIds;
    }

    private List<Long> distinct(Collection<Long> ids) {
        return ids == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private void fillAssignments(RoleVO vo) {
        List<Long> permissionIds = roleMapper.selectRolePermissionIds(vo.getId());
        List<Long> menuIds = roleMapper.selectRoleMenuIds(vo.getId());
        vo.setPermissionIds(permissionIds);
        vo.setMenuIds(menuIds);
        vo.setPermissions(permissionIds.isEmpty() ? List.of() : permissionMapper.selectBatchIds(permissionIds).stream()
                .map(Permission::getCode).toList());
        vo.setMenus(menuIds.isEmpty() ? List.of() : menuMapper.selectBatchIds(menuIds).stream()
                .map(Menu::getName).toList());
    }
}
