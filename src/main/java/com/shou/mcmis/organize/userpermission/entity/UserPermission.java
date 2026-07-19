package com.shou.mcmis.organize.userpermission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shou.mcmis.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_permission")
public class UserPermission extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long permissionId;
    private Integer effect;
    private LocalDateTime validFrom;
    private LocalDateTime expiresAt;
    private String reason;
    private Long grantBy;
}
