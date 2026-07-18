package com.shou.lims.organize.permission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PermissionCreateDTO {
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "权限名称不能超过64位")
    private String name;

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 64, message = "权限编码不能超过64位")
    @Pattern(regexp = "^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$",
            message = "权限编码格式必须为module:resource:action")
    private String code;

    @NotNull(message = "权限类型不能为空")
    @Min(value = 1, message = "权限类型不正确")
    @Max(value = 2, message = "权限类型不正确")
    private Integer type;
    @PositiveOrZero(message = "父权限ID不能为负数")
    private Long parentId;
    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder;
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;
}
