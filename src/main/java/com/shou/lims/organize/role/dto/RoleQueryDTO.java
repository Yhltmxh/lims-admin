package com.shou.lims.organize.role.dto;

import com.shou.lims.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryDTO extends PageQuery {
    private String name;
    private Integer status;
}
