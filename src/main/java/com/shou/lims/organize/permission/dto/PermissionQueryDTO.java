package com.shou.lims.organize.permission.dto;

import com.shou.lims.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionQueryDTO extends PageQuery {
    private String name;
    private String code;
    private Integer status;
}
