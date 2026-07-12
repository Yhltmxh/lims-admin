package com.shou.lims.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String cipherPwd;

    @NotBlank(message = "密钥ID不能为空")
    private String keyId;
}
