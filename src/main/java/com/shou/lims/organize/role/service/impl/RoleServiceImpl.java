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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleConverter roleConverter;

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
        if (role == null || StatusEnum.DISABLED.getValue().equals(role.getStatus())) {
            throw new NotFoundException("角色不存在");
        }
        return roleConverter.toVO(role);
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
            assignPermissions(role.getId(), dto.getPermissionIds());
        }
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), dto.getMenuIds());
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
        if (StringUtils.isNotBlank(dto.getLabel())) role.setLabel(dto.getLabel());
        if (dto.getDescription() != null) role.setDescription(dto.getDescription());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        roleMapper.updateById(role);

        if (dto.getPermissionIds() != null) {
            assignPermissions(id, dto.getPermissionIds());
        }
        if (dto.getMenuIds() != null) {
            assignMenus(id, dto.getMenuIds());
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        roleMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        roleMapper.deleteRolePermissions(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                roleMapper.insertRolePermission(roleId, permissionId);
            }
        }
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                roleMapper.insertRoleMenu(roleId, menuId);
            }
        }
    }
}
