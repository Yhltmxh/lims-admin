package com.shou.mcmis.organize.role.dto;

import com.shou.mcmis.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryDTO extends PageQuery {
    private String name;
    private Integer status;
}
