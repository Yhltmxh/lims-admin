package com.shou.lims.organize.userpermission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPermissionCreateDTO {
    @NotNull(message = "权限ID不能为空")
    @Positive(message = "权限ID必须为正数")
    private Long permissionId;
    @NotNull(message = "授权效果不能为空")
    @Min(value = 1, message = "授权效果不正确")
    @Max(value = 2, message = "授权效果不正确")
    private Integer effect;
    private LocalDateTime validFrom;
    private LocalDateTime expiresAt;
    @NotBlank(message = "授权原因不能为空")
    @Size(max = 256, message = "授权原因不能超过256位")
    private String reason;
}
