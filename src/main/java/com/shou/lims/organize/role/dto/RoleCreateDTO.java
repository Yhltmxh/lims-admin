package com.shou.lims.organize.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleCreateDTO {
    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色标签不能为空")
    private String label;

    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private List<Long> permissionIds;
    private List<Long> menuIds;
}
