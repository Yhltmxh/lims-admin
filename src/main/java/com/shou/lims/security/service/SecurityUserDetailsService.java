package com.shou.lims.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.organize.permission.entity.Permission;
import com.shou.lims.organize.permission.mapper.PermissionMapper;
import com.shou.lims.organize.role.entity.Role;
import com.shou.lims.organize.role.mapper.RoleMapper;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDelete, 0));

        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        if (StatusEnum.DISABLED.getValue().equals(user.getStatus())) {
            throw new UsernameNotFoundException("用户已被禁用");
        }

        // Load roles for this user
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        List<String> roleCodes = roles.stream()
                .filter(r -> StatusEnum.ENABLED.getValue().equals(r.getStatus()))
                .map(Role::getName)
                .map(code -> code.startsWith("ROLE_") ? code : "ROLE_" + code)
                .toList();

        List<Permission> permissions = permissionMapper.selectByUserId(user.getId());

        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .filter(p -> StatusEnum.ENABLED.getValue().equals(p.getStatus()))
                .map(Permission::getCode)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Also add role codes as authorities (for hasRole checks)
        roleCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));

        return new SecurityUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                StatusEnum.ENABLED.getValue().equals(user.getStatus()),
                authorities
        );
    }
}
