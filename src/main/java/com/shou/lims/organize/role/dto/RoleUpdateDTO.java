package com.shou.lims.organize.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleUpdateDTO {
    private String label;
    private String description;
    private Integer status;
    private List<Long> permissionIds;
    private List<Long> menuIds;
}
