package com.shou.mcmis.organize.permission.dto;

import com.shou.mcmis.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionQueryDTO extends PageQuery {
    private String name;
    private String code;
    private Integer status;
}
