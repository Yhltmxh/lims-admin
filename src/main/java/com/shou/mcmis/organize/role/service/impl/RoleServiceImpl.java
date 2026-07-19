package com.shou.mcmis.organize.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.mcmis.common.enums.StatusEnum;
import com.shou.mcmis.common.exception.BusinessException;
import com.shou.mcmis.common.exception.NotFoundException;
import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.role.converter.RoleConverter;
import com.shou.mcmis.organize.role.dto.RoleCreateDTO;
import com.shou.mcmis.organize.role.dto.RoleQueryDTO;
import com.shou.mcmis.organize.role.dto.RoleUpdateDTO;
import com.shou.mcmis.organize.role.entity.Role;
import com.shou.mcmis.organize.role.mapper.RoleMapper;
import com.shou.mcmis.organize.role.service.RoleService;
import com.shou.mcmis.organize.role.vo.RoleVO;
import com.shou.mcmis.organize.permission.entity.Permission;
import com.shou.mcmis.organize.permission.mapper.PermissionMapper;
import com.shou.mcmis.organize.menu.entity.Menu;
import com.shou.mcmis.organize.menu.mapper.MenuMapper;
import com.shou.mcmis.organize.user.mapper.UserRoleMapper;
import com.shou.mcmis.common.constant.GlobalConstants;
import com.shou.mcmis.security.service.EffectivePermissionService;
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
    private final EffectivePermissionService effectivePermissionService;

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
        if (GlobalConstants.SUPER_ADMIN_ROLE.equals(role.getName())
                && StatusEnum.DISABLED.getValue().equals(dto.getStatus())) {
            throw new BusinessException(400, "超级管理员角色不能禁用");
        }
        if (StringUtils.isNotBlank(dto.getLabel())) role.setLabel(dto.getLabel());
        if (dto.getDescription() != null) role.setDescription(dto.getDescription());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        if (roleMapper.updateById(role) == 0) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
        effectivePermissionService.invalidateAll(userRoleMapper.selectUserIdsByRoleIds(List.of(id)));

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
        if (roleMapper.selectBatchIds(ids).stream()
                .anyMatch(role -> GlobalConstants.SUPER_ADMIN_ROLE.equals(role.getName()))) {
            throw new BusinessException(400, "超级管理员角色不能删除");
        }
        List<Long> affectedUserIds = userRoleMapper.selectUserIdsByRoleIds(ids);
        userRoleMapper.deleteByRoleIds(ids);
        ids.forEach(id -> {
            roleMapper.deleteRolePermissions(id);
            roleMapper.deleteRoleMenus(id);
        });
        roleMapper.deleteBatchIds(ids);
        effectivePermissionService.invalidateAll(affectedUserIds);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        requireRole(roleId);
        List<Long> affectedUserIds = userRoleMapper.selectUserIdsByRoleIds(List.of(roleId));
        List<Long> validIds = validatePermissionIds(permissionIds);
        roleMapper.deleteRolePermissions(roleId);
        if (!validIds.isEmpty()) {
            for (Long permissionId : validIds) {
                roleMapper.insertRolePermission(roleId, permissionId);
            }
        }
        effectivePermissionService.invalidateAll(affectedUserIds);
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
