package com.shou.lims.organize.dept.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptCreateDTO {
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String name;

    private Integer sortOrder;
    private String leader;
    private String phone;
    private Integer status;
}
