package com.shou.mcmis.organize.menu.dto;

import com.shou.mcmis.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MenuQueryDTO extends PageQuery {
    private String name;
    private Integer status;
}
