package com.shou.lims.security.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.shou.lims.security.jwt.JwtTokenService;
import com.shou.lims.security.jwt.AccessTokenBlacklistService;
import com.shou.lims.security.service.SecurityUserDetails;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.mapper.UserMapper;
import com.shou.lims.organize.permission.entity.Permission;
import com.shou.lims.organize.permission.mapper.PermissionMapper;
import com.shou.lims.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AccessTokenBlacklistService accessTokenBlacklistService;
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (StringUtils.isBlank(header) || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            if (accessTokenBlacklistService.isBlacklisted(token)) {
                filterChain.doFilter(request, response);
                return;
            }
            DecodedJWT jwt = jwtTokenService.verifyAccessToken(token);

            Long userId = Long.valueOf(jwt.getSubject());
            User user = userMapper.selectById(userId);
            if (user == null || !StatusEnum.ENABLED.getValue().equals(user.getStatus())) {
                filterChain.doFilter(request, response);
                return;
            }
            String username = user.getUsername();
            List<SimpleGrantedAuthority> authorities = permissionMapper.selectByUserId(userId).stream()
                    .map(Permission::getCode)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            SecurityUserDetails userDetails = new SecurityUserDetails(
                    userId, username, "", true, authorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UnauthorizedException | NumberFormatException e) {
            // Token invalid or expired — let SecurityContext remain anonymous
            // The AuthenticationEntryPoint will handle the 401 response
        }

        filterChain.doFilter(request, response);
    }
}
