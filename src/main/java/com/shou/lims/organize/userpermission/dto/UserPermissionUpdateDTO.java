package com.shou.lims.organize.userpermission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPermissionUpdateDTO {
    @Min(value = 1, message = "授权效果不正确")
    @Max(value = 2, message = "授权效果不正确")
    private Integer effect;
    private LocalDateTime validFrom;
    private LocalDateTime expiresAt;
    @NotBlank(message = "变更原因不能为空")
    @Size(max = 256, message = "变更原因不能超过256位")
    private String reason;
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
}
