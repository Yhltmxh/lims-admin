package com.shou.mcmis.security.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shou.mcmis.common.constant.GlobalConstants;
import com.shou.mcmis.common.enums.StatusEnum;
import com.shou.mcmis.common.exception.UnauthorizedException;
import com.shou.mcmis.organize.permission.entity.Permission;
import com.shou.mcmis.organize.permission.mapper.PermissionMapper;
import com.shou.mcmis.organize.role.entity.Role;
import com.shou.mcmis.organize.role.mapper.RoleMapper;
import com.shou.mcmis.organize.user.entity.User;
import com.shou.mcmis.organize.user.mapper.UserMapper;
import com.shou.mcmis.organize.userpermission.entity.UserPermission;
import com.shou.mcmis.organize.userpermission.enums.PermissionEffect;
import com.shou.mcmis.organize.userpermission.mapper.UserPermissionMapper;
import com.shou.mcmis.security.service.EffectivePermissionService;
import com.shou.mcmis.security.service.EffectivePermissionSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EffectivePermissionServiceImpl implements EffectivePermissionService {
    private static final long MAX_CACHE_SECONDS = 300;
    private static final Set<String> LIST_DEPENDENT_ACTIONS = Set.of(
            "add", "edit", "del", "audit", "force-logout");
    private static final Map<String, String> EXPLICIT_DEPENDENCIES = Map.of(
            "organize:user-permission:list", "organize:user:list");

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserPermissionMapper userPermissionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public EffectivePermissionSnapshot resolve(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        EffectivePermissionSnapshot cached = getCached(userId, now);
        if (cached != null) {
            return cached;
        }

        User user = userMapper.selectById(userId);
        if (user == null || !StatusEnum.ENABLED.getValue().equals(user.getStatus())) {
            throw new UnauthorizedException();
        }

        List<Role> enabledRoles = roleMapper.selectByUserId(userId);
        Set<String> roleCodes = enabledRoles.stream().map(Role::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean superAdmin = roleCodes.contains(GlobalConstants.SUPER_ADMIN_ROLE);

        Set<String> rolePermissions = permissionMapper.selectByUserId(userId).stream()
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<UserPermission> directPermissions = userPermissionMapper.selectAllByUserId(userId);
        LocalDateTime nextBoundary = findNextBoundary(directPermissions, now);

        Set<String> allows = new LinkedHashSet<>();
        Set<String> denies = new LinkedHashSet<>();
        if (!superAdmin && !directPermissions.isEmpty()) {
            Set<Long> permissionIds = directPermissions.stream().map(UserPermission::getPermissionId)
                    .collect(Collectors.toSet());
            Map<Long, Permission> permissionMap = permissionMapper.selectBatchIds(permissionIds).stream()
                    .filter(p -> StatusEnum.ENABLED.getValue().equals(p.getStatus()))
                    .collect(Collectors.toMap(Permission::getId, Function.identity()));
            for (UserPermission direct : directPermissions) {
                if (!isEffective(direct, now)) {
                    continue;
                }
                Permission permission = permissionMap.get(direct.getPermissionId());
                if (permission == null) {
                    continue;
                }
                if (PermissionEffect.DENY.getValue() == direct.getEffect()) {
                    denies.add(permission.getCode());
                } else if (PermissionEffect.ALLOW.getValue() == direct.getEffect()) {
                    allows.add(permission.getCode());
                }
            }
        }

        Set<String> enabledCodes = new LinkedHashSet<>(permissionMapper.selectAllEnabledCodes());
        Set<String> effective = superAdmin
                ? new LinkedHashSet<>(enabledCodes)
                : new LinkedHashSet<>(rolePermissions);
        if (!superAdmin) {
            effective.addAll(allows);
            expandDependencies(effective, enabledCodes);
            effective.removeAll(denies);
            pruneMissingDependencies(effective, enabledCodes);
        }

        EffectivePermissionSnapshot snapshot = new EffectivePermissionSnapshot(
                effective, rolePermissions, allows, denies, roleCodes, superAdmin, nextBoundary);
        cache(userId, snapshot, now);
        return snapshot;
    }

    @Override
    public void invalidate(Long userId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteCache(userId);
                }
            });
            return;
        }
        deleteCache(userId);
    }

    private void deleteCache(Long userId) {
        try {
            redisTemplate.delete(key(userId));
        } catch (RuntimeException e) {
            log.warn("清理用户 {} 权限缓存失败", userId, e);
        }
    }

    @Override
    public void invalidateAll(Collection<Long> userIds) {
        if (userIds == null) {
            return;
        }
        userIds.forEach(this::invalidate);
    }

    @Override
    public boolean isSuperAdmin(Long userId) {
        return resolve(userId).isSuperAdmin();
    }

    private EffectivePermissionSnapshot getCached(Long userId, LocalDateTime now) {
        try {
            String json = redisTemplate.opsForValue().get(key(userId));
            if (json == null) {
                return null;
            }
            EffectivePermissionSnapshot snapshot = objectMapper.readValue(json, EffectivePermissionSnapshot.class);
            if (snapshot.getNextBoundary() != null && !now.isBefore(snapshot.getNextBoundary())) {
                invalidate(userId);
                return null;
            }
            return snapshot;
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("读取用户 {} 权限缓存失败，将回源数据库", userId, e);
            return null;
        }
    }

    private void cache(Long userId, EffectivePermissionSnapshot snapshot, LocalDateTime now) {
        long ttl = MAX_CACHE_SECONDS;
        if (snapshot.getNextBoundary() != null) {
            ttl = Math.min(ttl, Math.max(1, Duration.between(now, snapshot.getNextBoundary()).getSeconds()));
        }
        try {
            redisTemplate.opsForValue().set(key(userId), objectMapper.writeValueAsString(snapshot),
                    Duration.ofSeconds(ttl));
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("写入用户 {} 权限缓存失败", userId, e);
        }
    }

    private LocalDateTime findNextBoundary(List<UserPermission> permissions, LocalDateTime now) {
        return permissions.stream()
                .flatMap(permission -> java.util.stream.Stream.of(
                        permission.getValidFrom(), permission.getExpiresAt()))
                .filter(java.util.Objects::nonNull)
                .map(time -> time.withNano(0))
                .filter(time -> time.isAfter(now))
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private boolean isEffective(UserPermission permission, LocalDateTime now) {
        LocalDateTime from = permission.getValidFrom();
        LocalDateTime expiresAt = permission.getExpiresAt();
        return (from == null || !now.isBefore(from)) && (expiresAt == null || now.isBefore(expiresAt));
    }

    private void expandDependencies(Set<String> permissions, Set<String> enabledCodes) {
        boolean changed;
        do {
            changed = false;
            for (String permission : List.copyOf(permissions)) {
                String dependency = dependencyOf(permission);
                if (dependency != null && enabledCodes.contains(dependency)) {
                    changed |= permissions.add(dependency);
                }
            }
        } while (changed);
    }

    private void pruneMissingDependencies(Set<String> permissions, Set<String> enabledCodes) {
        boolean changed;
        do {
            changed = permissions.removeIf(permission -> {
                String dependency = dependencyOf(permission);
                return dependency != null
                        && (!enabledCodes.contains(dependency) || !permissions.contains(dependency));
            });
        } while (changed);
    }

    private String dependencyOf(String permission) {
        String explicit = EXPLICIT_DEPENDENCIES.get(permission);
        if (explicit != null) {
            return explicit;
        }
        int separator = permission.lastIndexOf(':');
        if (separator < 0 || !LIST_DEPENDENT_ACTIONS.contains(permission.substring(separator + 1))) {
            return null;
        }
        String listPermission = permission.substring(0, separator) + ":list";
        return listPermission;
    }

    private String key(Long userId) {
        return GlobalConstants.REDIS_AUTHORIZATION_PREFIX + userId;
    }
}
