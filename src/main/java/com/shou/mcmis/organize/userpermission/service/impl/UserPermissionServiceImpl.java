package com.shou.mcmis.organize.userpermission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shou.mcmis.common.constant.GlobalConstants;
import com.shou.mcmis.common.enums.StatusEnum;
import com.shou.mcmis.common.exception.BusinessException;
import com.shou.mcmis.common.exception.NotFoundException;
import com.shou.mcmis.common.util.SecurityUtils;
import com.shou.mcmis.organize.permission.entity.Permission;
import com.shou.mcmis.organize.permission.mapper.PermissionMapper;
import com.shou.mcmis.organize.user.entity.User;
import com.shou.mcmis.organize.user.mapper.UserMapper;
import com.shou.mcmis.organize.userpermission.dto.UserPermissionCreateDTO;
import com.shou.mcmis.organize.userpermission.dto.UserPermissionUpdateDTO;
import com.shou.mcmis.organize.userpermission.entity.UserPermission;
import com.shou.mcmis.organize.userpermission.entity.UserPermissionAudit;
import com.shou.mcmis.organize.userpermission.enums.PermissionEffect;
import com.shou.mcmis.organize.userpermission.mapper.UserPermissionAuditMapper;
import com.shou.mcmis.organize.userpermission.mapper.UserPermissionMapper;
import com.shou.mcmis.organize.userpermission.service.UserPermissionService;
import com.shou.mcmis.organize.userpermission.vo.EffectivePermissionVO;
import com.shou.mcmis.organize.userpermission.vo.UserPermissionAuditVO;
import com.shou.mcmis.organize.userpermission.vo.UserPermissionVO;
import com.shou.mcmis.organize.userpermission.vo.PermissionOptionVO;
import com.shou.mcmis.security.jwt.RefreshTokenService;
import com.shou.mcmis.security.service.EffectivePermissionService;
import com.shou.mcmis.security.service.EffectivePermissionSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {
    private final UserPermissionMapper userPermissionMapper;
    private final UserPermissionAuditMapper auditMapper;
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final EffectivePermissionService effectivePermissionService;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public List<UserPermissionVO> list(Long userId) {
        requireUser(userId);
        List<UserPermission> grants = userPermissionMapper.selectAllByUserId(userId);
        if (grants.isEmpty()) {
            return List.of();
        }
        Set<Long> permissionIds = grants.stream().map(UserPermission::getPermissionId).collect(Collectors.toSet());
        Set<Long> grantByIds = grants.stream().map(UserPermission::getGrantBy).collect(Collectors.toSet());
        Map<Long, Permission> permissions = permissionMapper.selectBatchIds(permissionIds).stream()
                .collect(Collectors.toMap(Permission::getId, Function.identity()));
        Map<Long, User> grantors = userMapper.selectBatchIds(grantByIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        LocalDateTime now = now();
        return grants.stream().map(grant -> toVO(grant, permissions.get(grant.getPermissionId()),
                grantors.get(grant.getGrantBy()), now)).toList();
    }

    @Override
    public EffectivePermissionVO effective(Long userId) {
        EffectivePermissionSnapshot snapshot = effectivePermissionService.resolve(userId);
        return new EffectivePermissionVO(snapshot.getPermissions(), snapshot.getRolePermissions(),
                snapshot.getAllowPermissions(), snapshot.getDenyPermissions(), snapshot.getRoles(),
                snapshot.isSuperAdmin(), snapshot.getNextBoundary());
    }

    @Override
    @Transactional
    public Long create(Long userId, UserPermissionCreateDTO dto) {
        requireUser(userId);
        requirePermission(dto.getPermissionId());
        rejectSuperAdminDeny(userId, dto.getEffect());
        LocalDateTime from = seconds(dto.getValidFrom());
        LocalDateTime expires = seconds(dto.getExpiresAt());
        validateTime(from, expires);
        validateNoOverlap(userId, dto.getPermissionId(), from, expires, null);

        UserPermission grant = new UserPermission();
        grant.setUserId(userId);
        grant.setPermissionId(dto.getPermissionId());
        grant.setEffect(dto.getEffect());
        grant.setValidFrom(from);
        grant.setExpiresAt(expires);
        grant.setReason(dto.getReason().trim());
        grant.setGrantBy(SecurityUtils.getCurrentUserId());
        try {
            userPermissionMapper.insert(grant);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(409, "授权时间段与现有配置重叠");
        }
        writeAudit("CREATE", null, grant, dto.getReason());
        effectivePermissionService.invalidate(userId);
        return grant.getId();
    }

    @Override
    @Transactional
    public void update(Long userId, Long grantId, UserPermissionUpdateDTO dto) {
        UserPermission grant = requireGrant(userId, grantId);
        if (dto.getVersion() != null && !dto.getVersion().equals(grant.getVersion())) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
        UserPermission before = copy(grant);
        Integer effect = dto.getEffect() == null ? grant.getEffect() : dto.getEffect();
        rejectSuperAdminDeny(userId, effect);
        LocalDateTime from = seconds(dto.getValidFrom());
        LocalDateTime expires = seconds(dto.getExpiresAt());
        validateTime(from, expires);
        validateNoOverlap(userId, grant.getPermissionId(), from, expires, grantId);
        grant.setEffect(effect);
        grant.setValidFrom(from);
        grant.setExpiresAt(expires);
        grant.setReason(dto.getReason().trim());
        grant.setGrantBy(SecurityUtils.getCurrentUserId());
        try {
            if (userPermissionMapper.updateById(grant) == 0) {
                throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(409, "授权时间段与现有配置重叠");
        }
        writeAudit("UPDATE", before, grant, dto.getReason());
        effectivePermissionService.invalidate(userId);
    }

    @Override
    @Transactional
    public void revoke(Long userId, Long grantId, String reason) {
        UserPermission grant = requireGrant(userId, grantId);
        if (userPermissionMapper.deleteById(grantId) == 0) {
            throw new BusinessException(409, "授权已被撤销，请刷新后重试");
        }
        writeAudit("REVOKE", grant, null, reason);
        effectivePermissionService.invalidate(userId);
    }

    @Override
    public List<UserPermissionAuditVO> audits(Long userId) {
        requireUser(userId);
        return auditMapper.selectByUserId(userId).stream().map(this::toAuditVO).toList();
    }

    @Override
    @Transactional
    public void forceLogout(Long userId, String reason) {
        requireUser(userId);
        if (userMapper.incrementAuthVersion(userId) == 0) {
            throw new NotFoundException("用户不存在");
        }
        refreshTokenService.revoke(userId);
        effectivePermissionService.invalidate(userId);
    }

    @Override
    public List<PermissionOptionVO> permissionOptions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getStatus, StatusEnum.ENABLED.getValue())
                        .orderByAsc(Permission::getCode))
                .stream().map(permission -> new PermissionOptionVO(
                        permission.getId(), permission.getName(), permission.getCode()))
                .toList();
    }

    private void rejectSuperAdminDeny(Long userId, Integer effect) {
        if (PermissionEffect.DENY.getValue() == effect && effectivePermissionService.isSuperAdmin(userId)) {
            throw new BusinessException(400, "超级管理员不能配置DENY权限");
        }
    }

    private void validateNoOverlap(Long userId, Long permissionId, LocalDateTime from,
                                   LocalDateTime expires, Long excludeId) {
        if (userPermissionMapper.countOverlaps(userId, permissionId, from, expires, excludeId) > 0) {
            throw new BusinessException(409, "授权时间段与现有配置重叠");
        }
    }

    private void validateTime(LocalDateTime from, LocalDateTime expires) {
        if (from != null && expires != null && !expires.isAfter(from)) {
            throw new BusinessException(400, "过期时间必须晚于生效时间");
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    private Permission requirePermission(Long permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null || !StatusEnum.ENABLED.getValue().equals(permission.getStatus())) {
            throw new BusinessException(400, "权限不存在或已禁用");
        }
        return permission;
    }

    private UserPermission requireGrant(Long userId, Long grantId) {
        UserPermission grant = userPermissionMapper.selectOne(new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getId, grantId).eq(UserPermission::getUserId, userId));
        if (grant == null) {
            throw new NotFoundException("用户直接授权不存在");
        }
        return grant;
    }

    private LocalDateTime seconds(LocalDateTime value) {
        return value == null ? null : value.withNano(0);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private boolean effective(UserPermission grant, LocalDateTime now) {
        return (grant.getValidFrom() == null || !now.isBefore(grant.getValidFrom()))
                && (grant.getExpiresAt() == null || now.isBefore(grant.getExpiresAt()));
    }

    private UserPermissionVO toVO(UserPermission grant, Permission permission, User grantor, LocalDateTime now) {
        UserPermissionVO vo = new UserPermissionVO();
        vo.setId(grant.getId());
        vo.setUserId(grant.getUserId());
        vo.setPermissionId(grant.getPermissionId());
        vo.setPermissionName(permission == null ? null : permission.getName());
        vo.setPermissionCode(permission == null ? null : permission.getCode());
        vo.setEffect(grant.getEffect());
        vo.setValidFrom(grant.getValidFrom());
        vo.setExpiresAt(grant.getExpiresAt());
        vo.setReason(grant.getReason());
        vo.setGrantBy(grant.getGrantBy());
        vo.setGrantByName(grantor == null ? null : grantor.getRealName());
        vo.setEffective(permission != null && StatusEnum.ENABLED.getValue().equals(permission.getStatus())
                && effective(grant, now));
        vo.setVersion(grant.getVersion());
        vo.setCreateTime(grant.getCreateTime());
        return vo;
    }

    private UserPermission copy(UserPermission source) {
        return objectMapper.convertValue(source, UserPermission.class);
    }

    private void writeAudit(String operation, UserPermission before, UserPermission after, String reason) {
        UserPermission reference = after != null ? after : before;
        UserPermissionAudit audit = baseAudit(reference.getUserId(), reference.getPermissionId(), operation, reason);
        audit.setGrantId(reference.getId());
        try {
            audit.setBeforeData(before == null ? null : objectMapper.writeValueAsString(before));
            audit.setAfterData(after == null ? null : objectMapper.writeValueAsString(after));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("权限审计序列化失败", e);
        }
        auditMapper.insertAudit(audit);
    }

    private UserPermissionAudit baseAudit(Long userId, Long permissionId, String operation, String reason) {
        UserPermissionAudit audit = new UserPermissionAudit();
        audit.setUserId(userId);
        audit.setPermissionId(permissionId == null ? 0L : permissionId);
        audit.setOperation(operation);
        audit.setReason(reason.trim());
        audit.setOperatorId(SecurityUtils.getCurrentUserId());
        audit.setOperatorUsername(SecurityUtils.getCurrentUsername());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            audit.setRequestId(request.getHeader("X-Request-Id"));
            audit.setIp(request.getRemoteAddr());
        }
        return audit;
    }

    private UserPermissionAuditVO toAuditVO(UserPermissionAudit audit) {
        return objectMapper.convertValue(audit, UserPermissionAuditVO.class);
    }
}
