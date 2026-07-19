package com.shou.mcmis.organize.userpermission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_permission_audit")
public class UserPermissionAudit {
    @TableId(type = IdType.AUTO)
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
