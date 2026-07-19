package com.shou.mcmis.security.service.impl;

import com.shou.mcmis.common.enums.StatusEnum;
import com.shou.mcmis.common.exception.UnauthorizedException;
import com.shou.mcmis.common.util.SecurityUtils;
import com.shou.mcmis.organize.user.entity.User;
import com.shou.mcmis.organize.user.mapper.UserMapper;
import com.shou.mcmis.security.jwt.JwtTokenService;
import com.shou.mcmis.security.jwt.RefreshTokenService;
import com.shou.mcmis.security.jwt.AccessTokenBlacklistService;
import com.shou.mcmis.security.service.AuthService;
import com.shou.mcmis.security.service.SecurityUserDetails;
import com.shou.mcmis.security.service.EffectivePermissionService;
import com.shou.mcmis.security.service.EffectivePermissionSnapshot;
import com.shou.mcmis.security.vo.LoginVO;
import com.shou.mcmis.security.vo.UserInfoVO;
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
    private final EffectivePermissionService effectivePermissionService;

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
                userDetails.getUserId(), userDetails.getUsername(), permissions,
                userDetails.getAuthVersion());
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
        if (!user.getAuthVersion().equals(jwtTokenService.extractVerifiedAuthVersionForRefresh(accessToken))) {
            refreshTokenService.revoke(userId);
            throw new UnauthorizedException();
        }
        EffectivePermissionSnapshot snapshot = effectivePermissionService.resolve(userId);
        List<String> permissionCodes = snapshot.getPermissions().stream().toList();

        String newAccessToken = jwtTokenService.generateAccessToken(
                userId, user.getUsername(), permissionCodes, user.getAuthVersion());
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

        EffectivePermissionSnapshot snapshot = effectivePermissionService.resolve(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(snapshot.getRoles().stream().toList());
        vo.setPermissions(snapshot.getPermissions().stream().toList());
        vo.setNextPermissionBoundary(snapshot.getNextBoundary());
        return vo;
    }
}
