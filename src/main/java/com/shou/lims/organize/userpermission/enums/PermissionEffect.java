package com.shou.lims.organize.userpermission.enums;

import lombok.Getter;

@Getter
public enum PermissionEffect {
    ALLOW(1), DENY(2);

    private final int value;

    PermissionEffect(int value) {
        this.value = value;
    }
}
