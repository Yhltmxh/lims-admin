package com.shou.lims.organize.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.common.exception.BusinessException;
import com.shou.lims.common.exception.NotFoundException;
import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.permission.converter.PermissionConverter;
import com.shou.lims.organize.permission.dto.PermissionCreateDTO;
import com.shou.lims.organize.permission.dto.PermissionQueryDTO;
import com.shou.lims.organize.permission.dto.PermissionUpdateDTO;
import com.shou.lims.organize.permission.entity.Permission;
import com.shou.lims.organize.permission.mapper.PermissionMapper;
import com.shou.lims.organize.permission.service.PermissionService;
import com.shou.lims.organize.permission.vo.PermissionVO;
import com.shou.lims.organize.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionConverter permissionConverter;
    private final RoleMapper roleMapper;

    @Override
    public PageVO<PermissionVO> page(PermissionQueryDTO query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<Permission>()
                .like(StringUtils.isNotBlank(query.getName()), Permission::getName, query.getName())
                .eq(StringUtils.isNotBlank(query.getCode()), Permission::getCode, query.getCode())
                .eq(query.getStatus() != null, Permission::getStatus, query.getStatus())
                .orderByAsc(Permission::getSortOrder);
        List<Permission> list = permissionMapper.selectList(wrapper);
        PageInfo<Permission> pageInfo = new PageInfo<>(list);
        return PageVO.of(pageInfo.convert(permissionConverter::toVO));
    }

    @Override
    public PermissionVO getById(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new NotFoundException("权限不存在");
        }
        return permissionConverter.toVO(permission);
    }

    @Override
    @Transactional
    public Long create(PermissionCreateDTO dto) {
        validateParent(null, dto.getParentId());
        Permission existing = permissionMapper.selectOne(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getCode, dto.getCode()));
        if (existing != null) {
            throw new BusinessException(409, "权限编码已存在");
        }
        Permission permission = permissionConverter.toEntity(dto);
        permission.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusEnum.ENABLED.getValue());
        permissionMapper.insert(permission);
        return permission.getId();
    }

    @Override
    @Transactional
    public void update(Long id, PermissionUpdateDTO dto) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new NotFoundException("权限不存在");
        }
        if (dto.getVersion() != null && !dto.getVersion().equals(permission.getVersion())) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
        if (dto.getParentId() != null) {
            validateParent(id, dto.getParentId());
        }
        if (StringUtils.isNotBlank(dto.getCode())) {
            Permission duplicate = permissionMapper.selectOne(new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getCode, dto.getCode()).ne(Permission::getId, id));
            if (duplicate != null) {
                throw new BusinessException(409, "权限编码已存在");
            }
        }
        if (StatusEnum.DISABLED.getValue().equals(dto.getStatus())
                && permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getParentId, id)
                .eq(Permission::getStatus, StatusEnum.ENABLED.getValue())) > 0) {
            throw new BusinessException(400, "请先禁用子权限");
        }
        if (StringUtils.isNotBlank(dto.getName())) permission.setName(dto.getName());
        if (StringUtils.isNotBlank(dto.getCode())) permission.setCode(dto.getCode());
        if (dto.getType() != null) permission.setType(dto.getType());
        if (dto.getParentId() != null) permission.setParentId(dto.getParentId());
        if (dto.getSortOrder() != null) permission.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) permission.setStatus(dto.getStatus());
        if (permissionMapper.updateById(permission) == 0) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(ids));
        long childCount = permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                .in(Permission::getParentId, distinctIds)
                .notIn(Permission::getId, distinctIds));
        if (childCount > 0) {
            throw new BusinessException(400, "存在未同时删除的子权限");
        }
        roleMapper.deleteRolePermissionsByPermissionIds(distinctIds);
        permissionMapper.deleteBatchIds(distinctIds);
    }

    private void validateParent(Long currentId, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (parentId.equals(currentId)) {
            throw new BusinessException(400, "上级权限不能是自身");
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L) {
            if (!visited.add(cursor) || cursor.equals(currentId)) {
                throw new BusinessException(400, "权限层级不能形成循环");
            }
            Permission parent = permissionMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException(400, "上级权限不存在");
            }
            if (!StatusEnum.ENABLED.getValue().equals(parent.getStatus())) {
                throw new BusinessException(400, "上级权限已禁用");
            }
            cursor = parent.getParentId();
        }
    }
}
