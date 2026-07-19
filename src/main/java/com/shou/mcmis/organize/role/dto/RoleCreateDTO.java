package com.shou.mcmis.organize.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RoleCreateDTO {
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称不能超过32位")
    private String name;

    @NotBlank(message = "角色标签不能为空")
    @Size(max = 32, message = "角色标签不能超过32位")
    private String label;

    @Size(max = 128, message = "角色描述不能超过128位")
    private String description;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;

    @Size(max = 500, message = "一次最多分配500个权限")
    private List<@Positive(message = "权限ID必须为正数") Long> permissionIds;

    @Size(max = 500, message = "一次最多分配500个菜单")
    private List<@Positive(message = "菜单ID必须为正数") Long> menuIds;
}
