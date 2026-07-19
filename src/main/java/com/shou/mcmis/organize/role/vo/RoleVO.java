package com.shou.mcmis.organize.role.vo;

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
    private Integer version;
    private LocalDateTime createTime;
    private List<Long> permissionIds;
    private List<Long> menuIds;
    private List<String> permissions;
    private List<String> menus;
}
