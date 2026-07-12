package com.shou.lims.organize.menu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuCreateDTO {
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String name;

    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer hidden;
    private Integer status;
}
