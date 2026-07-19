package com.shou.mcmis.organize.dept.dto;

import com.shou.mcmis.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeptQueryDTO extends PageQuery {
    private String name;
    private Integer status;
}
