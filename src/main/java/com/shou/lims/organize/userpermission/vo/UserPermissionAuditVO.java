package com.shou.lims.organize.userpermission.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPermissionAuditVO {
    private Long id;
    private Long grantId;
    private Long userId;
    private Long permissionId;
    private String operation;
    private String beforeData;
    private String afterData;
    private String reason;
    private Long operatorId;
    private String operatorUsername;
    private String requestId;
    private String ip;
    private LocalDateTime createTime;
}
