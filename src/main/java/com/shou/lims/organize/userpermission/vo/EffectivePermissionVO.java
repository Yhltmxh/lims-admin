package com.shou.lims.organize.userpermission.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
public class EffectivePermissionVO {
    private Set<String> permissions;
    private Set<String> rolePermissions;
    private Set<String> allowPermissions;
    private Set<String> denyPermissions;
    private Set<String> roles;
    private boolean superAdmin;
    private LocalDateTime nextBoundary;
}
