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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionConverter permissionConverter;

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
        if (permission == null || StatusEnum.DISABLED.getValue().equals(permission.getStatus())) {
            throw new NotFoundException("权限不存在");
        }
        return permissionConverter.toVO(permission);
    }

    @Override
    @Transactional
    public Long create(PermissionCreateDTO dto) {
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
        if (StringUtils.isNotBlank(dto.getName())) permission.setName(dto.getName());
        if (StringUtils.isNotBlank(dto.getCode())) permission.setCode(dto.getCode());
        if (dto.getType() != null) permission.setType(dto.getType());
        if (dto.getParentId() != null) permission.setParentId(dto.getParentId());
        if (dto.getSortOrder() != null) permission.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) permission.setStatus(dto.getStatus());
        permissionMapper.updateById(permission);
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        permissionMapper.deleteBatchIds(ids);
    }
}
