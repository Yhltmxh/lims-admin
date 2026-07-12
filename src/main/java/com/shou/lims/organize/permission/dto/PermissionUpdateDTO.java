package com.shou.lims.organize.permission.dto;

import lombok.Data;

@Data
public class PermissionUpdateDTO {
    private String name;
    private String code;
    private Integer type;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
}
