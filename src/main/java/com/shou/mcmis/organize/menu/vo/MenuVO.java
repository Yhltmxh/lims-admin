package com.shou.mcmis.organize.menu.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MenuVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer hidden;
    private Integer status;
    private Long requiredPermissionId;
    private Integer version;
    private List<MenuVO> children;
    private LocalDateTime createTime;
}
