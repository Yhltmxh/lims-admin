package com.shou.lims.security.controller;

import com.shou.lims.common.response.Result;
import com.shou.lims.security.dto.LoginRequest;
import com.shou.lims.security.dto.RefreshRequest;
import com.shou.lims.security.service.AuthService;
import com.shou.lims.security.service.RsaKeyService;
import com.shou.lims.security.vo.LoginVO;
import com.shou.lims.security.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {

    private final AuthService authService;
    private final RsaKeyService rsaKeyService;
    private final Environment environment;

    @GetMapping("/public-key")
    @Operation(summary = "获取RSA公钥")
    public Result<RsaKeyService.RsaKeyPair> getPublicKey() {
        return Result.success(rsaKeyService.generateKeyPair());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录（dev环境可不传keyId，cipherPwd直接作为明文密码）")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        String rawPassword;
        if (StringUtils.isBlank(request.getKeyId())) {
            // dev环境允许明文密码，方便Swagger调试
            if (environment.matchesProfiles("dev")) {
                log.debug("dev环境明文密码登录: {}", request.getUsername());
                rawPassword = request.getCipherPwd();
            } else {
                return Result.fail(400, "密钥ID不能为空");
            }
        } else {
            rawPassword = rsaKeyService.decrypt(request.getKeyId(), request.getCipherPwd());
        }
        return Result.success(authService.login(request.getUsername(), rawPassword));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public Result<LoginVO> refresh(@RequestHeader("Authorization") String authHeader,
                                   @Valid @RequestBody RefreshRequest request) {
        String token = authHeader.replace("Bearer ", "");
        return Result.success(authService.refresh(token, request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "注销")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoVO> me() {
        return Result.success(authService.getCurrentUserInfo());
    }
}
