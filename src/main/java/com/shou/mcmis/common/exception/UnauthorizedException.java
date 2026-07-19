package com.shou.mcmis.common.exception;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException() {
        super(401, "未登录或Token已过期");
    }
}
