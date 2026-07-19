package com.shou.mcmis.organize.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.mcmis.common.enums.StatusEnum;
import com.shou.mcmis.common.exception.BusinessException;
import com.shou.mcmis.common.exception.NotFoundException;
import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.user.converter.UserConverter;
import com.shou.mcmis.organize.user.dto.UserCreateDTO;
import com.shou.mcmis.organize.user.dto.UserQueryDTO;
import com.shou.mcmis.organize.user.dto.UserUpdateDTO;
import com.shou.mcmis.organize.user.entity.User;
import com.shou.mcmis.organize.user.mapper.UserMapper;
import com.shou.mcmis.organize.user.mapper.UserRoleMapper;
import com.shou.mcmis.organize.user.mapper.UserRoleRow;
import com.shou.mcmis.organize.user.service.UserService;
import com.shou.mcmis.organize.user.vo.UserVO;
import com.shou.mcmis.organize.dept.entity.Dept;
import com.shou.mcmis.organize.dept.mapper.DeptMapper;
import com.shou.mcmis.organize.role.entity.Role;
import com.shou.mcmis.organize.role.mapper.RoleMapper;
import com.shou.mcmis.security.jwt.RefreshTokenService;
import com.shou.mcmis.common.util.SecurityUtils;
import com.shou.mcmis.common.constant.GlobalConstants;
import com.shou.mcmis.security.service.EffectivePermissionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final DeptMapper deptMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EffectivePermissionService effectivePermissionService;

    @Override
    public PageVO<UserVO> page(UserQueryDTO query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.isNotBlank(query.getUsername()), User::getUsername, query.getUsername())
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, User::getDeptId, query.getDeptId())
                .eq(User::getIsDelete, 0)
                .orderByDesc(User::getCreateTime);
        List<User> list = userMapper.selectList(wrapper);
        PageInfo<User> pageInfo = new PageInfo<>(list);
        List<UserVO> records = userConverter.toVOList(list);
        enrichUsers(list, records);
        PageVO<UserVO> result = PageVO.of(pageInfo.convert(userConverter::toVO));
        result.setRecords(records);
        return result;
    }

    @Override
    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        UserVO vo = userConverter.toVO(user);
        enrichUsers(List.of(user), List.of(vo));
        return vo;
    }

    @Override
    @Transactional
    public Long create(UserCreateDTO dto) {
        validateDepartment(dto.getDeptId());
        List<Long> roleIds = validateRoles(dto.getRoleIds(), true);

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .eq(User::getIsDelete, 0));
        if (existing != null) {
            throw new BusinessException(409, "用户名已存在");
        }
        User user = userConverter.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(StatusEnum.ENABLED.getValue());
        userMapper.insert(user);

        assignRoles(user.getId(), roleIds);
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        if (dto.getVersion() != null && !dto.getVersion().equals(user.getVersion())) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
        if (dto.getDeptId() != null) {
            validateDepartment(dto.getDeptId());
        }
        List<Long> roleIds = dto.getRoleIds() == null ? null : validateRoles(dto.getRoleIds(), true);
        if (id.equals(SecurityUtils.getCurrentUserId())
                && StatusEnum.DISABLED.getValue().equals(dto.getStatus())) {
            throw new BusinessException(400, "不能禁用当前登录用户");
        }
        if (StringUtils.isNotBlank(dto.getRealName())) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getDeptId() != null) user.setDeptId(dto.getDeptId());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }

        if (roleIds != null) {
            protectLastSuperAdmin(id, roleIds, dto.getStatus());
            assignRoles(id, roleIds);
        } else if (StatusEnum.DISABLED.getValue().equals(dto.getStatus())) {
            protectLastSuperAdmin(id, userRoleMapper.selectRoleIdsByUserId(id), dto.getStatus());
        }
        if (StatusEnum.DISABLED.getValue().equals(dto.getStatus())) {
            refreshTokenService.revoke(id);
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (ids.contains(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(400, "不能删除当前登录用户");
        }
        userRoleMapper.lockRoleByName(GlobalConstants.SUPER_ADMIN_ROLE);
        Set<Long> enabledDeletingUserIds = userMapper.selectBatchIds(ids).stream()
                .filter(user -> StatusEnum.ENABLED.getValue().equals(user.getStatus()))
                .map(User::getId)
                .collect(Collectors.toSet());
        long deletingSuperAdmins = enabledDeletingUserIds.stream().filter(id -> roleMapper.selectByUserId(id).stream()
                .anyMatch(role -> GlobalConstants.SUPER_ADMIN_ROLE.equals(role.getName()))).count();
        long enabledSuperAdmins = userRoleMapper.countEnabledUsersByRoleNameExcluding(
                GlobalConstants.SUPER_ADMIN_ROLE, null);
        if (deletingSuperAdmins > 0 && enabledSuperAdmins <= deletingSuperAdmins) {
            throw new BusinessException(400, "系统必须至少保留一个启用的超级管理员");
        }
        userRoleMapper.deleteByUserIds(ids);
        userMapper.deleteBatchIds(ids);
        ids.forEach(refreshTokenService::revoke);
        effectivePermissionService.invalidateAll(ids);
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        roleIds.forEach(roleId -> userRoleMapper.insert(userId, roleId));
        effectivePermissionService.invalidate(userId);
    }

    private void protectLastSuperAdmin(Long userId, List<Long> newRoleIds, Integer newStatus) {
        userRoleMapper.lockRoleByName(GlobalConstants.SUPER_ADMIN_ROLE);
        boolean currentlySuper = roleMapper.selectByUserId(userId).stream()
                .anyMatch(role -> GlobalConstants.SUPER_ADMIN_ROLE.equals(role.getName()));
        if (!currentlySuper) {
            return;
        }
        boolean remainsSuper = roleMapper.selectBatchIds(newRoleIds).stream()
                .anyMatch(role -> GlobalConstants.SUPER_ADMIN_ROLE.equals(role.getName()));
        boolean remainsEnabled = !StatusEnum.DISABLED.getValue().equals(newStatus);
        if ((!remainsSuper || !remainsEnabled)
                && userRoleMapper.countEnabledUsersByRoleNameExcluding(
                GlobalConstants.SUPER_ADMIN_ROLE, userId) == 0) {
            throw new BusinessException(400, "系统必须至少保留一个启用的超级管理员");
        }
    }

    private List<Long> validateRoles(Collection<Long> roleIds, boolean required) {
        if (roleIds == null || roleIds.isEmpty()) {
            if (required) {
                throw new BusinessException(400, "用户至少需要分配一个角色");
            }
            return List.of();
        }
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(roleIds));
        List<Role> roles = roleMapper.selectBatchIds(distinctIds);
        Set<Long> enabledRoleIds = roles.stream()
                .filter(role -> StatusEnum.ENABLED.getValue().equals(role.getStatus()))
                .map(Role::getId)
                .collect(Collectors.toSet());
        if (enabledRoleIds.size() != distinctIds.size() || !enabledRoleIds.containsAll(distinctIds)) {
            throw new BusinessException(400, "角色不存在或已禁用");
        }
        return distinctIds;
    }

    private void validateDepartment(Long deptId) {
        if (deptId == null) {
            return;
        }
        Dept dept = deptMapper.selectById(deptId);
        if (dept == null || !StatusEnum.ENABLED.getValue().equals(dept.getStatus())) {
            throw new BusinessException(400, "部门不存在或已禁用");
        }
    }

    private void enrichUsers(List<User> users, List<UserVO> records) {
        if (users.isEmpty()) {
            return;
        }
        Set<Long> deptIds = users.stream().map(User::getDeptId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Dept> deptMap = deptIds.isEmpty() ? Map.of() : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Dept::getId, Function.identity()));

        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, List<UserRoleRow>> roleRows = userRoleMapper.selectRoleRowsByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleRow::getUserId));

        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        for (UserVO vo : records) {
            User user = userMap.get(vo.getId());
            Dept dept = user != null && user.getDeptId() != null ? deptMap.get(user.getDeptId()) : null;
            vo.setDeptName(dept != null ? dept.getName() : null);
            List<UserRoleRow> rows = roleRows.getOrDefault(vo.getId(), List.of());
            vo.setRoleIds(rows.stream().map(UserRoleRow::getRoleId).toList());
            vo.setRoleNames(rows.stream().map(UserRoleRow::getRoleName).toList());
        }
    }
}
