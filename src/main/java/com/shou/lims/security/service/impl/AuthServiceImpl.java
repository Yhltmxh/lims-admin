package com.shou.lims.security.service.impl;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.shou.lims.common.exception.UnauthorizedException;
import com.shou.lims.common.util.SecurityUtils;
import com.shou.lims.organize.role.entity.Role;
import com.shou.lims.organize.role.mapper.RoleMapper;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.mapper.UserMapper;
import com.shou.lims.security.jwt.JwtTokenService;
import com.shou.lims.security.jwt.RefreshTokenService;
import com.shou.lims.security.service.AuthService;
import com.shou.lims.security.service.RsaKeyService;
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
    private final RsaKeyService rsaKeyService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

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

        return new LoginVO(accessToken, refreshToken, 900L);
    }

    @Override
    public LoginVO refresh(String accessToken, String refreshToken) {
        // Decode userId from the (possibly expired) access token without verification
        Long userId;
        try {
            userId = jwtTokenService.extractUserId(accessToken);
        } catch (JWTDecodeException | NumberFormatException e) {
            throw new UnauthorizedException();
        }

        if (!refreshTokenService.validate(userId, refreshToken)) {
            throw new UnauthorizedException();
        }

        // Revoke old refresh token (rotation)
        refreshTokenService.revoke(userId);

        User user = userMapper.selectById(userId);
        List<Role> roles = roleMapper.selectByUserId(userId);
        List<String> permissions = roles.stream()
                .map(Role::getName)
                .toList();

        String newAccessToken = jwtTokenService.generateAccessToken(
                userId, user.getUsername(), permissions);
        String newRefreshToken = jwtTokenService.generateRefreshToken();

        refreshTokenService.store(userId, newRefreshToken);

        return new LoginVO(newAccessToken, newRefreshToken, 900L);
    }

    @Override
    public void logout(String accessToken) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == 0L) {
            return; // anonymous, nothing to revoke
        }
        refreshTokenService.revoke(userId);

        // TODO: Blacklist access token via CacheService — will be fully implemented in Phase 5
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
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
