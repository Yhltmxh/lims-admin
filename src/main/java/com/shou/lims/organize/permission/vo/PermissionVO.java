package com.shou.lims.organize.permission.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermissionVO {
    private Long id;
    private String name;
    private String code;
    private Integer type;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
}
