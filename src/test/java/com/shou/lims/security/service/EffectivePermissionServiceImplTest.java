package com.shou.lims.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shou.lims.organize.permission.entity.Permission;
import com.shou.lims.organize.permission.mapper.PermissionMapper;
import com.shou.lims.organize.role.entity.Role;
import com.shou.lims.organize.role.mapper.RoleMapper;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.mapper.UserMapper;
import com.shou.lims.organize.userpermission.entity.UserPermission;
import com.shou.lims.organize.userpermission.enums.PermissionEffect;
import com.shou.lims.organize.userpermission.mapper.UserPermissionMapper;
import com.shou.lims.security.service.impl.EffectivePermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectivePermissionServiceImplTest {
    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private UserPermissionMapper userPermissionMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private EffectivePermissionService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), zone);
        now = LocalDateTime.now(clock);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new EffectivePermissionServiceImpl(userMapper, roleMapper, permissionMapper,
                userPermissionMapper, redisTemplate, objectMapper, clock);
        User user = new User();
        user.setId(2L);
        user.setStatus(1);
        when(userMapper.selectById(2L)).thenReturn(user);
    }

    @Test
    void shouldMergeRoleAllowAndDenyWithDenyHighestPriority() {
        Role role = role("ROLE_MANAGER");
        when(roleMapper.selectByUserId(2L)).thenReturn(List.of(role));
        Permission list = permission(1L, "organize:user:list");
        Permission add = permission(2L, "organize:user:add");
        when(permissionMapper.selectByUserId(2L)).thenReturn(List.of(list));
        when(permissionMapper.selectBatchIds(any())).thenReturn(List.of(list, add));
        when(permissionMapper.selectAllEnabledCodes()).thenReturn(
                List.of("organize:user:add", "organize:user:list"));

        UserPermission deny = grant(1L, PermissionEffect.DENY, now.minusHours(1), now.plusHours(1));
        UserPermission allow = grant(2L, PermissionEffect.ALLOW, now.minusHours(1), now.plusHours(2));
        when(userPermissionMapper.selectAllByUserId(2L)).thenReturn(List.of(deny, allow));

        EffectivePermissionSnapshot result = service.resolve(2L);

        assertThat(result.getPermissions()).isEmpty();
        assertThat(result.getDenyPermissions()).containsExactly("organize:user:list");
        assertThat(result.getAllowPermissions()).containsExactly("organize:user:add");
        assertThat(result.getNextBoundary()).isEqualTo(now.plusHours(1));
    }

    @Test
    void actionPermissionShouldImplyRequiredListPermissions() {
        when(roleMapper.selectByUserId(2L)).thenReturn(List.of(role("ROLE_OPERATOR")));
        Permission add = permission(2L, "organize:user-permission:add");
        when(permissionMapper.selectByUserId(2L)).thenReturn(List.of(add));
        when(permissionMapper.selectAllEnabledCodes()).thenReturn(List.of(
                "organize:user-permission:add",
                "organize:user-permission:list",
                "organize:user:list"));
        when(userPermissionMapper.selectAllByUserId(2L)).thenReturn(List.of());

        EffectivePermissionSnapshot result = service.resolve(2L);

        assertThat(result.getPermissions()).containsExactly(
                "organize:user-permission:add",
                "organize:user-permission:list",
                "organize:user:list");
    }

    @Test
    void actionPermissionShouldBeRemovedWhenItsListPermissionIsDisabled() {
        when(roleMapper.selectByUserId(2L)).thenReturn(List.of(role("ROLE_OPERATOR")));
        Permission add = permission(2L, "organize:dept:add");
        when(permissionMapper.selectByUserId(2L)).thenReturn(List.of(add));
        when(permissionMapper.selectAllEnabledCodes()).thenReturn(List.of("organize:dept:add"));
        when(userPermissionMapper.selectAllByUserId(2L)).thenReturn(List.of());

        EffectivePermissionSnapshot result = service.resolve(2L);

        assertThat(result.getPermissions()).isEmpty();
    }

    @Test
    void superAdminShouldIgnoreDirectDenyAndReceiveAllEnabledPermissions() {
        when(roleMapper.selectByUserId(2L)).thenReturn(List.of(role("ROLE_ADMIN")));
        Permission list = permission(1L, "organize:user:list");
        when(permissionMapper.selectByUserId(2L)).thenReturn(List.of(list));
        when(permissionMapper.selectAllEnabledCodes()).thenReturn(
                List.of("organize:user:add", "organize:user:list"));
        when(userPermissionMapper.selectAllByUserId(2L)).thenReturn(List.of(
                grant(1L, PermissionEffect.DENY, null, null)));

        EffectivePermissionSnapshot result = service.resolve(2L);

        assertThat(result.isSuperAdmin()).isTrue();
        assertThat(result.getPermissions()).containsExactly("organize:user:add", "organize:user:list");
        assertThat(result.getDenyPermissions()).isEmpty();
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        role.setStatus(1);
        return role;
    }

    private Permission permission(Long id, String code) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setStatus(1);
        return permission;
    }

    private UserPermission grant(Long permissionId, PermissionEffect effect,
                                 LocalDateTime from, LocalDateTime expiresAt) {
        UserPermission grant = new UserPermission();
        grant.setUserId(2L);
        grant.setPermissionId(permissionId);
        grant.setEffect(effect.getValue());
        grant.setValidFrom(from);
        grant.setExpiresAt(expiresAt);
        return grant;
    }
}
