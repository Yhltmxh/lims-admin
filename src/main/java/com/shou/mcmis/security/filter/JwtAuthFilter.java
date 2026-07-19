package com.shou.mcmis.security.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.shou.mcmis.security.jwt.JwtTokenService;
import com.shou.mcmis.security.jwt.AccessTokenBlacklistService;
import com.shou.mcmis.security.service.SecurityUserDetails;
import com.shou.mcmis.common.enums.StatusEnum;
import com.shou.mcmis.organize.user.entity.User;
import com.shou.mcmis.organize.user.mapper.UserMapper;
import com.shou.mcmis.security.service.EffectivePermissionService;
import com.shou.mcmis.security.service.EffectivePermissionSnapshot;
import com.shou.mcmis.common.exception.UnauthorizedException;
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
    private final EffectivePermissionService effectivePermissionService;
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
            Integer tokenAuthVersion = jwt.getClaim("authVersion").asInt();
            if (tokenAuthVersion == null || !tokenAuthVersion.equals(user.getAuthVersion())) {
                filterChain.doFilter(request, response);
                return;
            }
            String username = user.getUsername();
            EffectivePermissionSnapshot snapshot = effectivePermissionService.resolve(userId);
            List<SimpleGrantedAuthority> authorities = snapshot.getPermissions().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(java.util.stream.Collectors.toList());
            snapshot.getRoles().forEach(code -> authorities.add(new SimpleGrantedAuthority(
                    code.startsWith("ROLE_") ? code : "ROLE_" + code)));

            SecurityUserDetails userDetails = new SecurityUserDetails(
                    userId, username, "", true, authorities, user.getAuthVersion());

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
