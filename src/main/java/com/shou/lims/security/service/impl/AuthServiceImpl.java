package com.shou.lims.security.service.impl;

import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.common.exception.UnauthorizedException;
import com.shou.lims.organize.permission.entity.Permission;
import com.shou.lims.organize.permission.mapper.PermissionMapper;
import com.shou.lims.common.util.SecurityUtils;
import com.shou.lims.organize.role.entity.Role;
import com.shou.lims.organize.role.mapper.RoleMapper;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.mapper.UserMapper;
import com.shou.lims.security.jwt.JwtTokenService;
import com.shou.lims.security.jwt.RefreshTokenService;
import com.shou.lims.security.jwt.AccessTokenBlacklistService;
import com.shou.lims.security.service.AuthService;
import com.shou.lims.security.service.SecurityUserDetails;
import com.shou.lims.security.vo.LoginVO;
import com.shou.lims.security.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenBlacklistService accessTokenBlacklistService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public LoginVO login(String username, String rawPassword) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, rawPassword);
        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();

        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        String accessToken = jwtTokenService.generateAccessToken(
                userDetails.getUserId(), userDetails.getUsername(), permissions);
        String refreshToken = jwtTokenService.generateRefreshToken();

        refreshTokenService.store(userDetails.getUserId(), refreshToken);

        return new LoginVO(accessToken, refreshToken, jwtTokenService.getAccessTokenExpirationSeconds());
    }

    @Override
    public LoginVO refresh(String accessToken, String refreshToken) {
        Long userId = jwtTokenService.extractVerifiedUserIdForRefresh(accessToken);

        User user = userMapper.selectById(userId);
        if (user == null || !StatusEnum.ENABLED.getValue().equals(user.getStatus())) {
            refreshTokenService.revoke(userId);
            throw new UnauthorizedException();
        }
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        List<String> permissionCodes = permissions.stream()
                .filter(p -> StatusEnum.ENABLED.getValue().equals(p.getStatus()))
                .map(Permission::getCode)
                .toList();

        String newAccessToken = jwtTokenService.generateAccessToken(
                userId, user.getUsername(), permissionCodes);
        String newRefreshToken = jwtTokenService.generateRefreshToken();

        if (!refreshTokenService.rotate(userId, refreshToken, newRefreshToken)) {
            throw new UnauthorizedException();
        }

        return new LoginVO(newAccessToken, newRefreshToken, jwtTokenService.getAccessTokenExpirationSeconds());
    }

    @Override
    public void logout(String accessToken) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == 0L) {
            return; // anonymous, nothing to revoke
        }
        refreshTokenService.revoke(userId);
        accessTokenBlacklistService.blacklist(accessToken, jwtTokenService.getRemainingSeconds(accessToken));
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !StatusEnum.ENABLED.getValue().equals(user.getStatus())) {
            throw new UnauthorizedException();
        }

        List<Role> roles = roleMapper.selectByUserId(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roles.stream().map(Role::getName).toList());
        // Permissions come from SecurityContext
        vo.setPermissions(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList());
        return vo;
    }
}
