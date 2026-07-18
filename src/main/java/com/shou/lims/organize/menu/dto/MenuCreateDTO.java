package com.shou.lims.organize.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuCreateDTO {
    @PositiveOrZero(message = "父菜单ID不能为负数")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 32, message = "菜单名称不能超过32位")
    private String name;

    @Size(max = 128, message = "路由地址不能超过128位")
    private String path;
    @Size(max = 128, message = "组件地址不能超过128位")
    private String component;
    @Size(max = 32, message = "图标不能超过32位")
    private String icon;
    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder;
    @Min(value = 0, message = "隐藏标识不正确")
    @Max(value = 1, message = "隐藏标识不正确")
    private Integer hidden;
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;
    @PositiveOrZero(message = "所需权限ID不能为负数")
    private Long requiredPermissionId;
}
