package com.shou.mcmis.organize.userpermission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForceLogoutDTO {
    @NotBlank(message = "强制下线原因不能为空")
    @Size(max = 256, message = "强制下线原因不能超过256位")
    private String reason;
}
