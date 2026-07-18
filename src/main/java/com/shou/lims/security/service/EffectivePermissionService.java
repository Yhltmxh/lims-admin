package com.shou.lims.security.service;

import java.util.Collection;

public interface EffectivePermissionService {
    EffectivePermissionSnapshot resolve(Long userId);
    void invalidate(Long userId);
    void invalidateAll(Collection<Long> userIds);
    boolean isSuperAdmin(Long userId);
}
