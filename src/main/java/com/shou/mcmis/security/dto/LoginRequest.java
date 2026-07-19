package com.shou.mcmis.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String cipherPwd;

    /** dev环境可留空，此时cipherPwd直接作为明文密码 */
    private String keyId;
}
