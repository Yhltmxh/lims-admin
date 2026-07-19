package com.shou.mcmis.security.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EffectivePermissionSnapshot {
    private Set<String> permissions = new LinkedHashSet<>();
    private Set<String> rolePermissions = new LinkedHashSet<>();
    private Set<String> allowPermissions = new LinkedHashSet<>();
    private Set<String> denyPermissions = new LinkedHashSet<>();
    private Set<String> roles = new LinkedHashSet<>();
    private boolean superAdmin;
    private LocalDateTime nextBoundary;
}
