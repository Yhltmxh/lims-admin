package com.shou.lims.organize.dept.dto;

import lombok.Data;

@Data
public class DeptUpdateDTO {
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private Integer status;
}
