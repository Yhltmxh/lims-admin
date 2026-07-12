package com.shou.lims.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shou.lims.common.response.Result;
import com.shou.lims.security.jwt.JwtTokenService;
import com.shou.lims.security.jwt.RefreshTokenService;
import com.shou.lims.security.service.SecurityUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();

        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        String accessToken = jwtTokenService.generateAccessToken(
                userDetails.getUserId(), userDetails.getUsername(), permissions);
        String refreshToken = jwtTokenService.generateRefreshToken();

        refreshTokenService.store(userDetails.getUserId(), refreshToken);

        Map<String, Object> data = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "expiresIn", 900
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.success(data)));
    }
}
