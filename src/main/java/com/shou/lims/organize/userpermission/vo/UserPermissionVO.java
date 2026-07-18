package com.shou.lims.organize.userpermission.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPermissionVO {
    private Long id;
    private Long userId;
    private Long permissionId;
    private String permissionName;
    private String permissionCode;
    private Integer effect;
    private LocalDateTime validFrom;
    private LocalDateTime expiresAt;
    private String reason;
    private Long grantBy;
    private String grantByName;
    private boolean effective;
    private Integer version;
    private LocalDateTime createTime;
}
