package com.shou.lims.organize.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionCreateDTO {
    @NotBlank(message = "权限名称不能为空")
    private String name;

    @NotBlank(message = "权限编码不能为空")
    private String code;

    private Integer type;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
}
