package com.shou.lims.common.exception;

public class ForbiddenException extends BusinessException {
    public ForbiddenException() {
        super(403, "无访问权限");
    }
}
