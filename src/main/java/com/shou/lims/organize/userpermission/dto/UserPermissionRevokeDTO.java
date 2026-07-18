package com.shou.lims.organize.userpermission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPermissionRevokeDTO {
    @NotBlank(message = "撤销原因不能为空")
    @Size(max = 256, message = "撤销原因不能超过256位")
    private String reason;
}
