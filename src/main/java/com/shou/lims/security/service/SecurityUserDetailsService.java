package com.shou.lims.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final EffectivePermissionService effectivePermissionService;

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

        EffectivePermissionSnapshot snapshot = effectivePermissionService.resolve(user.getId());
        List<SimpleGrantedAuthority> authorities = snapshot.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
        snapshot.getRoles().forEach(code -> authorities.add(new SimpleGrantedAuthority(
                code.startsWith("ROLE_") ? code : "ROLE_" + code)));

        return new SecurityUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                StatusEnum.ENABLED.getValue().equals(user.getStatus()),
                authorities,
                user.getAuthVersion()
        );
    }
}
