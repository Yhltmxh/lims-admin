package com.shou.lims.organize.user.dto;

import com.shou.lims.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {
    private String username;
    private Integer status;
    private Long deptId;
}
