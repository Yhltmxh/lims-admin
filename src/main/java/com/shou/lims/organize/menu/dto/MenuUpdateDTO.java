package com.shou.lims.organize.menu.dto;

import lombok.Data;

@Data
public class MenuUpdateDTO {
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer hidden;
    private Integer status;
}
