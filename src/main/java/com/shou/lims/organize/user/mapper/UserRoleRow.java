package com.shou.lims.organize.user.mapper;

import lombok.Data;

/**
 * 用户角色关联查询的内部投影，不作为 API DTO 暴露。
 */
@Data
public class UserRoleRow {
    private Long userId;
    private Long roleId;
    private String roleName;
}
