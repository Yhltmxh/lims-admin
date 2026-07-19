package com.shou.mcmis.security.controller;

import com.shou.mcmis.common.response.Result;
import com.shou.mcmis.common.exception.UnauthorizedException;
import com.shou.mcmis.common.exception.BusinessException;
import com.shou.mcmis.security.dto.LoginRequest;
import com.shou.mcmis.security.dto.RefreshRequest;
import com.shou.mcmis.security.service.AuthService;
import com.shou.mcmis.security.service.RsaKeyService;
import com.shou.mcmis.security.vo.LoginVO;
import com.shou.mcmis.security.vo.UserInfoVO;
import com.shou.mcmis.organize.menu.service.MenuService;
import com.shou.mcmis.organize.menu.vo.MenuRouteVO;
import com.shou.mcmis.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {

    private final AuthService authService;
    private final RsaKeyService rsaKeyService;
    private final Environment environment;
    private final MenuService menuService;

    @GetMapping("/public-key")
    @Operation(summary = "获取RSA公钥", operationId = "getRsaPublicKey")
    @SecurityRequirements
    public Result<RsaKeyService.RsaKeyPair> getPublicKey() {
        return Result.success(rsaKeyService.generateKeyPair());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录（dev环境可不传keyId，cipherPwd直接作为明文密码）", operationId = "login")
    @SecurityRequirements
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        String rawPassword;
        if (StringUtils.isBlank(request.getKeyId())) {
            // dev环境允许明文密码，方便Swagger调试
            if (environment.matchesProfiles("dev", "test")) {
                log.debug("dev环境明文密码登录: {}", request.getUsername());
                rawPassword = request.getCipherPwd();
            } else {
                throw new BusinessException(400, "密钥ID不能为空");
            }
        } else {
            rawPassword = rsaKeyService.decrypt(request.getKeyId(), request.getCipherPwd());
        }
        return Result.success(authService.login(request.getUsername(), rawPassword));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", operationId = "refreshToken")
    public Result<LoginVO> refresh(@RequestHeader("Authorization") String authHeader,
                                   @Valid @RequestBody RefreshRequest request) {
        String token = extractBearerToken(authHeader);
        return Result.success(authService.refresh(token, request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "注销", operationId = "logout")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        authService.logout(token);
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", operationId = "getCurrentUser")
    @PreAuthorize("isAuthenticated()")
    public Result<UserInfoVO> me() {
        return Result.success(authService.getCurrentUserInfo());
    }

    @GetMapping("/menus")
    @Operation(summary = "获取当前用户菜单", operationId = "getCurrentUserMenus")
    @PreAuthorize("isAuthenticated()")
    public Result<List<MenuRouteVO>> menus() {
        return Result.success(menuService.getCurrentUserMenuTree(SecurityUtils.getCurrentUserId()));
    }

    private String extractBearerToken(String authHeader) {
        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith("Bearer ")
                || authHeader.length() == "Bearer ".length()) {
            throw new UnauthorizedException();
        }
        return authHeader.substring("Bearer ".length());
    }
}
