package com.shou.lims.organize.role.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO {
    private Long id;
    private String name;
    private String label;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private List<String> permissions;
    private List<String> menus;
}
